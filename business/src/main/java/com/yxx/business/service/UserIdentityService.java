package com.yxx.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxx.business.model.entity.UserIdentity;

import java.util.Optional;

/**
 * 用户登录身份服务。
 */
public interface UserIdentityService extends IService<UserIdentity> {

    /**
     * 按身份类型和唯一标识查找登录身份。
     *
     * @param identityType 身份类型
     * @param identifier   唯一标识
     * @return 登录身份
     */
    Optional<UserIdentity> findIdentity(String identityType, String identifier);

    /**
     * 查找用户绑定的指定类型身份。
     *
     * @param userId       系统用户主键
     * @param identityType 身份类型
     * @return 登录身份
     */
    Optional<UserIdentity> findByUserId(Long userId, String identityType);
}
