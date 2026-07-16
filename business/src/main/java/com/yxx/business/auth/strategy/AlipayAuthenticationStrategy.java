package com.yxx.business.auth.strategy;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.yxx.business.auth.command.AlipayAuthenticationCommand;
import com.yxx.business.auth.command.UserAuthenticationCommand;
import com.yxx.business.auth.model.AuthenticatedUser;
import com.yxx.business.model.entity.User;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.common.utils.ApiAssert;
import com.yxx.common.utils.ServletUtils;
import com.yxx.common.utils.ip.ClientIpResolver;
import com.yxx.security.constant.LoginMode;
import com.yxx.security.constant.SecurityRealm;
import com.yxx.security.context.PasswordLoginProtectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * 支付宝授权认证策略。
 *
 * <p>只信任后端使用一次性授权码从支付宝换取的用户编号，禁止直接接收前端提交的支付宝
 * userId 作为登录凭据。首次登录会创建统一系统用户、支付宝身份和默认角色。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "features.alipay-login", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class AlipayAuthenticationStrategy implements UserAuthenticationStrategy {

    private final AlipayClient alipayClient;
    private final AlipayIdentityBindingService identityBindingService;
    private final PasswordLoginProtectionService loginProtectionService;
    private final ClientIpResolver clientIpResolver;

    @Override
    public String loginMode() {
        return LoginMode.ALIPAY;
    }

    @Override
    public AuthenticatedUser authenticate(UserAuthenticationCommand command) {
        // 前端只提交一次性授权码，支付宝用户 ID 必须由服务端向支付宝换取。
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID, command instanceof AlipayAuthenticationCommand);
        String authCode = ((AlipayAuthenticationCommand) command).authCode();
        String clientIp = clientIpResolver.resolve(ServletUtils.getRequest());
        // 支付宝授权接口同样执行账号摘要与 IP 双维度频控，防止随机授权码消耗外部接口资源。
        loginProtectionService.reserveAttempt(SecurityRealm.USER, "alipay:" + authCode, clientIp);
        String alipayUserId = exchangeUserId(authCode);
        User user;
        try {
            // 常规路径在独立事务中查询或创建统一主体及身份绑定。
            user = identityBindingService.bindOrLoad(alipayUserId);
        } catch (DataIntegrityViolationException exception) {
            // 两个首次登录请求可能同时通过存在性检查，唯一键会保证只有一个事务成功。
            // 失败请求在原事务回滚后重新读取已完成的绑定，避免向客户端暴露数据库异常。
            if (!isConcurrentIdentityConflict(exception)) {
                throw exception;
            }
            user = identityBindingService.loadBoundUser(alipayUserId);
        }
        loginProtectionService.recordSuccess(
                SecurityRealm.USER, "alipay:" + authCode, clientIp);
        return new AuthenticatedUser(user, systemAccount(user.getId()), loginMode());
    }

    private String exchangeUserId(String authCode) {
        // 使用支付宝标准 authorization_code 授权模式兑换用户身份。
        AlipaySystemOauthTokenRequest request = new AlipaySystemOauthTokenRequest();
        request.setGrantType("authorization_code");
        request.setCode(authCode);
        try {
            AlipaySystemOauthTokenResponse response = alipayClient.execute(request);
            if (response.isSuccess() && response.getUserId() != null) {
                // 只接受明确成功且包含 userId 的响应，避免将空身份带入本地绑定流程。
                return response.getUserId();
            }
            log.warn("支付宝授权失败，code={}，subCode={}", response.getCode(), response.getSubCode());
            throw new ApiException(ApiCode.ALIPAY_AUTHENTICATION_FAILED);
        } catch (AlipayApiException exception) {
            log.error("调用支付宝授权接口失败", exception);
            throw new ApiException(ApiCode.EXTERNAL_SERVICE_UNAVAILABLE, exception);
        }
    }

    private boolean isConcurrentIdentityConflict(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause().getMessage();
        return message != null && message.contains("uk_user_identity_active_identifier");
    }

    private String systemAccount(Long userId) {
        // 支付宝身份没有本地登录账号，使用稳定内部 ID 生成仅用于主体展示的系统账号。
        return "user:" + userId;
    }
}
