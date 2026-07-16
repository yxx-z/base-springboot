package com.yxx.business.service;

import com.yxx.business.model.entity.UserIdentity;

import java.util.Optional;

/**
 * 用户登录身份服务。
 */
public interface UserIdentityService {

    /** 创建登录身份。 */
    boolean create(UserIdentity identity);

    /** 更新密码身份的凭据摘要。 */
    boolean updateCredential(Long identityId, String encodedCredential);

    /**
     * 按身份类型和唯一标识查找任意状态的登录身份，用于区分“身份不存在”和“身份不可用”。
     *
     * @param identityType 身份类型
     * @param identifier   唯一标识
     * @return 登录身份
     */
    Optional<UserIdentity> findAnyIdentity(String identityType, String identifier);

    /**
     * 查找用户绑定的指定类型身份。
     *
     * @param userId       系统用户主键
     * @param identityType 身份类型
     * @return 登录身份
     */
    Optional<UserIdentity> findByUserId(Long userId, String identityType);
}
