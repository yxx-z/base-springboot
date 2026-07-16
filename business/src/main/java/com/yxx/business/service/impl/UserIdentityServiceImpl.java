package com.yxx.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
    public boolean create(UserIdentity identity) {
        return save(identity);
    }

    @Override
    public boolean updateCredential(Long identityId, String encodedCredential) {
        // 定向更新凭据字段，避免使用旧身份实体覆盖验证状态或其他并发变更。
        return update(new LambdaUpdateWrapper<UserIdentity>()
                .eq(UserIdentity::getId, identityId)
                .set(UserIdentity::getCredential, encodedCredential));
    }

    @Override
    public Optional<UserIdentity> findAnyIdentity(String identityType, String identifier) {
        // “任意身份”包含停用或未验证记录，供认证流程在凭据正确后返回精确状态。
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<UserIdentity>()
                .eq(UserIdentity::getIdentityType, identityType)
                .eq(UserIdentity::getIdentifier, identifier)));
    }

    @Override
    public Optional<UserIdentity> findByUserId(Long userId, String identityType) {
        // 业务操作只使用已验证且启用的身份，防止对不可用凭据执行修改或重置。
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<UserIdentity>()
                .eq(UserIdentity::getUserId, userId)
                .eq(UserIdentity::getIdentityType, identityType)
                .eq(UserIdentity::getVerified, Boolean.TRUE)
                .eq(UserIdentity::getStatus, Boolean.TRUE)));
    }
}
