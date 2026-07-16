package com.yxx.business.auth.strategy;

import com.yxx.business.model.entity.User;
import com.yxx.business.model.entity.UserIdentity;
import com.yxx.business.service.UserIdentityService;
import com.yxx.business.service.UserRoleService;
import com.yxx.business.service.UserService;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.utils.ApiAssert;
import com.yxx.security.constant.LoginMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付宝身份绑定应用服务。
 *
 * <p>授权码兑换属于外部网络调用，不应占用数据库事务；本服务只负责事务内的主体、身份和
 * 默认角色写入。数据库唯一键仍是最终并发安全边界，调用方会在并发冲突后重新查询。</p>
 */
@Service
@RequiredArgsConstructor
public class AlipayIdentityBindingService {

    private final UserIdentityService userIdentityService;
    private final UserService userService;
    private final UserRoleService userRoleService;

    @Transactional(rollbackFor = Exception.class)
    public User bindOrLoad(String alipayUserId) {
        UserIdentity identity = userIdentityService.findAnyIdentity(LoginMode.ALIPAY, alipayUserId)
                .orElse(null);
        if (identity != null) {
            ApiAssert.isTrue(ApiCode.IDENTITY_DISABLED, Boolean.TRUE.equals(identity.getStatus()));
            ApiAssert.isTrue(ApiCode.IDENTITY_NOT_VERIFIED, Boolean.TRUE.equals(identity.getVerified()));
            User user = userService.findById(identity.getUserId());
            ApiAssert.isTrue(ApiCode.USER_NOT_EXIST,
                    user != null && Boolean.TRUE.equals(user.getStatus()));
            return user;
        }

        User user = new User();
        user.setDisplayName("支付宝用户");
        user.setStatus(Boolean.TRUE);
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, userService.create(user));

        UserIdentity newIdentity = new UserIdentity();
        newIdentity.setUserId(user.getId());
        newIdentity.setIdentityType(LoginMode.ALIPAY);
        newIdentity.setIdentifier(alipayUserId);
        newIdentity.setVerified(Boolean.TRUE);
        newIdentity.setStatus(Boolean.TRUE);
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, userIdentityService.create(newIdentity));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, userRoleService.assignMemberRole(user));
        return user;
    }

    /**
     * 首次登录并发写入发生唯一键冲突后，重新读取已经由另一事务完成的绑定结果。
     */
    public User loadBoundUser(String alipayUserId) {
        UserIdentity identity = userIdentityService.findAnyIdentity(LoginMode.ALIPAY, alipayUserId)
                .orElse(null);
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, identity != null);
        ApiAssert.isTrue(ApiCode.IDENTITY_DISABLED, Boolean.TRUE.equals(identity.getStatus()));
        ApiAssert.isTrue(ApiCode.IDENTITY_NOT_VERIFIED, Boolean.TRUE.equals(identity.getVerified()));
        User user = userService.findById(identity.getUserId());
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST,
                user != null && Boolean.TRUE.equals(user.getStatus()));
        return user;
    }
}
