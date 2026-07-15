package com.yxx.business.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.business.mapper.UserRoleMapper;
import com.yxx.business.model.entity.Role;
import com.yxx.business.model.entity.User;
import com.yxx.business.model.entity.UserRole;
import com.yxx.business.service.RoleService;
import com.yxx.business.service.UserRoleService;
import com.yxx.business.security.UserSecurityCodes;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.utils.ApiAssert;
import com.yxx.security.context.SessionInvalidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * @author yxx
 * @since 2023-05-17 10:01
 */
@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {
    private final RoleService roleService;

    private final SessionInvalidationService sessionInvalidationService;

    @Override
    public Boolean setDefaultRole(User user) {
        // 根据角色code 获取角色详情
        Role role = roleService.getOne(
                new LambdaQueryWrapper<Role>().eq(Role::getCode, UserSecurityCodes.ROLE_MEMBER));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, ObjectUtil.isNotNull(role));
        // 初始化用户角色实体类
        UserRole userRole = new UserRole();
        // 设置用户id
        userRole.setUserId(user.getId());
        // 设置角色id
        userRole.setRoleId(role.getId());
        // 保存
        return save(userRole);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceRoles(Long userId, Collection<Integer> roleIds) {
        // 关联表采用物理删除，确保同一角色可以在撤销后再次分配。
        remove(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (roleIds != null && !roleIds.isEmpty()) {
            List<UserRole> relations = roleIds.stream().distinct().map(roleId -> {
                UserRole relation = new UserRole();
                relation.setUserId(userId);
                relation.setRoleId(roleId);
                return relation;
            }).toList();
            saveBatch(relations);
        }
        // 当前项目采用登录时权限快照，角色变更后必须注销旧会话。
        sessionInvalidationService.invalidateUserAfterCommit(userId);
    }

    @Override
    public List<Long> listUserIdsByRoleId(Integer roleId) {
        return list(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId))
                .stream()
                .map(UserRole::getUserId)
                .distinct()
                .toList();
    }

    @Override
    public List<Integer> listRoleIdsByUserId(Long userId) {
        return list(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
                .stream()
                .map(UserRole::getRoleId)
                .distinct()
                .toList();
    }
}
