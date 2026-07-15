package com.yxx.business.auth.strategy;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.yxx.business.auth.command.AlipayAuthenticationCommand;
import com.yxx.business.auth.command.UserAuthenticationCommand;
import com.yxx.business.auth.model.AuthenticatedUser;
import com.yxx.business.model.entity.User;
import com.yxx.business.model.entity.UserIdentity;
import com.yxx.business.service.UserIdentityService;
import com.yxx.business.service.UserRoleService;
import com.yxx.business.service.UserService;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.common.utils.ApiAssert;
import com.yxx.security.constant.LoginMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserIdentityService userIdentityService;
    private final UserService userService;
    private final UserRoleService userRoleService;

    @Override
    public String loginMode() {
        return LoginMode.ALIPAY;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthenticatedUser authenticate(UserAuthenticationCommand command) {
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID, command instanceof AlipayAuthenticationCommand);
        String alipayUserId = exchangeUserId(((AlipayAuthenticationCommand) command).authCode());

        UserIdentity existingIdentity = userIdentityService
                .findIdentity(LoginMode.ALIPAY, alipayUserId)
                .orElse(null);
        if (existingIdentity != null) {
            User existingUser = userService.getById(existingIdentity.getUserId());
            ApiAssert.isTrue(ApiCode.USER_NOT_EXIST,
                    existingUser != null && Boolean.TRUE.equals(existingUser.getStatus()));
            return new AuthenticatedUser(existingUser, systemAccount(existingUser.getId()), loginMode());
        }

        // 第三方平台首次登录时先创建系统主体，再绑定外部身份。后续业务数据始终关联 user.id。
        User user = new User();
        user.setDisplayName("支付宝用户");
        user.setStatus(Boolean.TRUE);
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, userService.save(user));

        UserIdentity identity = new UserIdentity();
        identity.setUserId(user.getId());
        identity.setIdentityType(LoginMode.ALIPAY);
        identity.setIdentifier(alipayUserId);
        identity.setVerified(Boolean.TRUE);
        identity.setStatus(Boolean.TRUE);
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, userIdentityService.save(identity));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, userRoleService.setDefaultRole(user));
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
