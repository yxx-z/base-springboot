package com.yxx.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.business.mapper.UserIdentityMapper;
import com.yxx.business.model.entity.UserIdentity;
import com.yxx.business.service.UserIdentityService;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 用户登录身份服务实现。
 */
@Service
public class UserIdentityServiceImpl extends ServiceImpl<UserIdentityMapper, UserIdentity>
        implements UserIdentityService {

    @Override
    public Optional<UserIdentity> findIdentity(String identityType, String identifier) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<UserIdentity>()
                .eq(UserIdentity::getIdentityType, identityType)
                .eq(UserIdentity::getIdentifier, identifier)
                .eq(UserIdentity::getVerified, Boolean.TRUE)
                .eq(UserIdentity::getStatus, Boolean.TRUE)));
    }

    @Override
    public Optional<UserIdentity> findByUserId(Long userId, String identityType) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<UserIdentity>()
                .eq(UserIdentity::getUserId, userId)
                .eq(UserIdentity::getIdentityType, identityType)
                .eq(UserIdentity::getVerified, Boolean.TRUE)
                .eq(UserIdentity::getStatus, Boolean.TRUE)));
    }
}
