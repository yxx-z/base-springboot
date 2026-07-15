package com.yxx.admin.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.admin.mapper.AdminUserRoleMapper;
import com.yxx.admin.model.entity.AdminRole;
import com.yxx.admin.model.entity.AdminUser;
import com.yxx.admin.model.entity.AdminUserRole;
import com.yxx.admin.service.AdminRoleService;
import com.yxx.admin.service.AdminUserRoleService;
import com.yxx.admin.security.AdminSecurityCodes;
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
public class AdminUserRoleServiceImpl extends ServiceImpl<AdminUserRoleMapper, AdminUserRole> implements AdminUserRoleService {
    private final AdminRoleService adminRoleService;

    private final SessionInvalidationService sessionInvalidationService;

    @Override
    public Boolean setDefaultRole(AdminUser user) {
        // 根据角色code 获取角色详情
        AdminRole adminRole = adminRoleService.getOne(
                new LambdaQueryWrapper<AdminRole>()
                        .eq(AdminRole::getCode, AdminSecurityCodes.ROLE_ADMINISTRATOR));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, ObjectUtil.isNotNull(adminRole));
        // 初始化用户角色实体类
        AdminUserRole adminUserRole = new AdminUserRole();
        // 设置用户id
        adminUserRole.setUserId(user.getId());
        // 设置角色id
        adminUserRole.setRoleId(adminRole.getId());
        // 保存
        return save(adminUserRole);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceRoles(Long userId, Collection<Integer> roleIds) {
        remove(new LambdaQueryWrapper<AdminUserRole>().eq(AdminUserRole::getUserId, userId));
        if (roleIds != null && !roleIds.isEmpty()) {
            List<AdminUserRole> relations = roleIds.stream().distinct().map(roleId -> {
                AdminUserRole relation = new AdminUserRole();
                relation.setUserId(userId);
                relation.setRoleId(roleId);
                return relation;
            }).toList();
            saveBatch(relations);
        }
        sessionInvalidationService.invalidateAdminAfterCommit(userId);
    }

    @Override
    public List<Long> listUserIdsByRoleId(Integer roleId) {
        return list(new LambdaQueryWrapper<AdminUserRole>().eq(AdminUserRole::getRoleId, roleId))
                .stream()
                .map(AdminUserRole::getUserId)
                .distinct()
                .toList();
    }

    @Override
    public List<Integer> listRoleIdsByUserId(Long userId) {
        return list(new LambdaQueryWrapper<AdminUserRole>().eq(AdminUserRole::getUserId, userId))
                .stream()
                .map(AdminUserRole::getRoleId)
                .distinct()
                .toList();
    }
}
