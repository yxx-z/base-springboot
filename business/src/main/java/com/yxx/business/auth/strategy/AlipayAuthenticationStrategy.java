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
import com.yxx.security.constant.LoginMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * 支付宝授权认证策略。
 *
 * <p>只信任后端使用一次性授权码从支付宝换取的用户编号，禁止直接接收前端提交的支付宝
 * userId 作为登录凭据。首次登录会创建统一系统用户、支付宝身份和默认角色。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayAuthenticationStrategy implements UserAuthenticationStrategy {

    private final AlipayClient alipayClient;
    private final AlipayIdentityBindingService identityBindingService;

    @Override
    public String loginMode() {
        return LoginMode.ALIPAY;
    }

    @Override
    public AuthenticatedUser authenticate(UserAuthenticationCommand command) {
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID, command instanceof AlipayAuthenticationCommand);
        String alipayUserId = exchangeUserId(((AlipayAuthenticationCommand) command).authCode());
        User user;
        try {
            user = identityBindingService.bindOrLoad(alipayUserId);
        } catch (DataIntegrityViolationException exception) {
            // 两个首次登录请求可能同时通过存在性检查，唯一键会保证只有一个事务成功。
            // 失败请求在原事务回滚后重新读取已完成的绑定，避免向客户端暴露数据库异常。
            user = identityBindingService.loadBoundUser(alipayUserId);
        }
        return new AuthenticatedUser(user, systemAccount(user.getId()), loginMode());
    }

    private String exchangeUserId(String authCode) {
        AlipaySystemOauthTokenRequest request = new AlipaySystemOauthTokenRequest();
        request.setGrantType("authorization_code");
        request.setCode(authCode);
        try {
            AlipaySystemOauthTokenResponse response = alipayClient.execute(request);
            if (response.isSuccess() && response.getUserId() != null) {
                return response.getUserId();
            }
            log.warn("支付宝授权失败，code={}，subCode={}", response.getCode(), response.getSubCode());
            throw new ApiException(ApiCode.SYSTEM_ERROR);
        } catch (AlipayApiException exception) {
            log.error("调用支付宝授权接口失败", exception);
            throw new ApiException(ApiCode.SYSTEM_ERROR, exception);
        }
    }

    private String systemAccount(Long userId) {
        return "user:" + userId;
    }
}
