package com.yxx.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.admin.mapper.AdminUserRoleMapper;
import com.yxx.admin.mapper.AdminUserMapper;
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

    private final AdminUserMapper adminUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceRoles(Long userId, Collection<Integer> roleIds) {
        // 在改变关联前确认管理员存在，并验证目标角色集合中的每一个 ID。
        AdminUser targetUser = adminUserMapper.selectById(userId);
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST, targetUser != null);
        // 去重避免重复关联触发唯一约束；空集合表示撤销全部普通角色。
        List<Integer> distinctRoleIds = roleIds == null
                ? List.of()
                : roleIds.stream().distinct().toList();
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID,
                adminRoleService.findByIds(distinctRoleIds).size() == distinctRoleIds.size());

        // 禁止移除系统中最后一个超级管理员，避免管理端进入无法恢复的无最高权限状态。
        AdminRole superAdminRole = adminRoleService.findByCode(AdminSecurityCodes.ROLE_SUPER_ADMIN)
                .orElse(null);
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, superAdminRole != null);
        boolean targetIsSuperAdmin = count(new LambdaQueryWrapper<AdminUserRole>()
                .eq(AdminUserRole::getUserId, userId)
                .eq(AdminUserRole::getRoleId, superAdminRole.getId())) > 0;
        // 只有“启用的超级管理员将失去超级角色”才可能减少可用最高权限人数。
        boolean keepsSuperAdminRole = distinctRoleIds.contains(superAdminRole.getId());
        if (targetIsSuperAdmin && Boolean.TRUE.equals(targetUser.getStatus()) && !keepsSuperAdminRole) {
            long superAdminCount = adminUserMapper.countActiveUsersByRoleCode(
                    AdminSecurityCodes.ROLE_SUPER_ADMIN);
            ApiAssert.isTrue(ApiCode.LAST_SUPER_ADMIN, superAdminCount > 1);
        }

        // 校验通过后以最终集合替换旧关联，整个过程受事务保护。
        remove(new LambdaQueryWrapper<AdminUserRole>().eq(AdminUserRole::getUserId, userId));
        if (!distinctRoleIds.isEmpty()) {
            List<AdminUserRole> relations = distinctRoleIds.stream().map(roleId -> {
                AdminUserRole relation = new AdminUserRole();
                relation.setUserId(userId);
                relation.setRoleId(roleId);
                return relation;
            }).toList();
            ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, saveBatch(relations));
        }
        // 角色变化会改变登录快照，提交后注销该管理员的全部旧会话。
        sessionInvalidationService.invalidateAdminAfterCommit(userId);
    }

    @Override
    public List<Long> listUserIdsByRoleId(Integer roleId) {
        // 去重可容忍历史脏数据，避免重复注销同一个管理员。
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
