package com.yxx.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yxx.business.mapper.UserIdentityMapper;
import com.yxx.business.model.entity.UserIdentity;
import com.yxx.business.service.UserIdentityService;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * 用户登录身份服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserIdentityServiceImpl implements UserIdentityService {

    private final UserIdentityMapper identityMapper;

    @Override
    public boolean create(UserIdentity identity) {
        return identityMapper.insert(identity) == 1;
    }

    @Override
    public boolean updateCredential(Long identityId, String encodedCredential) {
        // 定向更新凭据字段，避免使用旧身份实体覆盖验证状态或其他并发变更。
        return identityMapper.update(new LambdaUpdateWrapper<UserIdentity>()
                .eq(UserIdentity::getId, identityId)
                .set(UserIdentity::getCredential, encodedCredential)) == 1;
    }

    @Override
    public boolean deleteByUserId(Long userId) {
        // MyBatis-Plus 根据 @TableLogic 生成逻辑删除更新，不会物理清除历史身份标识。
        identityMapper.delete(new LambdaQueryWrapper<UserIdentity>()
                .eq(UserIdentity::getUserId, userId));
        return true;
    }

    @Override
    public boolean hasActivePasswordIdentity(Long userId) {
        return identityMapper.selectCount(new LambdaQueryWrapper<UserIdentity>()
                .eq(UserIdentity::getUserId, userId)
                .eq(UserIdentity::getIdentityType, com.yxx.security.constant.LoginMode.PASSWORD)
                .eq(UserIdentity::getVerified, Boolean.TRUE)
                .eq(UserIdentity::getStatus, Boolean.TRUE)) > 0;
    }

    @Override
    public Optional<UserIdentity> findAnyIdentity(String identityType, String identifier) {
        // “任意身份”包含停用或未验证记录，供认证流程在凭据正确后返回精确状态。
        return Optional.ofNullable(identityMapper.selectOne(new LambdaQueryWrapper<UserIdentity>()
                .eq(UserIdentity::getIdentityType, identityType)
                .eq(UserIdentity::getIdentifier, identifier)));
    }

    @Override
    public Optional<UserIdentity> findByUserId(Long userId, String identityType) {
        // 业务操作只使用已验证且启用的身份，防止对不可用凭据执行修改或重置。
        return Optional.ofNullable(identityMapper.selectOne(new LambdaQueryWrapper<UserIdentity>()
                .eq(UserIdentity::getUserId, userId)
                .eq(UserIdentity::getIdentityType, identityType)
                .eq(UserIdentity::getVerified, Boolean.TRUE)
                .eq(UserIdentity::getStatus, Boolean.TRUE)));
    }
}
