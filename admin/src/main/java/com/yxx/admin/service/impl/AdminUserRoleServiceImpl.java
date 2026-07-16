package com.yxx.admin.service.impl;

import com.yxx.admin.mapper.AdminUserMapper;
import com.yxx.admin.model.entity.AdminUser;
import com.yxx.admin.service.AdminUserRoleService;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.utils.ApiAssert;
import com.yxx.rbac.model.RbacScope;
import com.yxx.rbac.constant.RbacSecurityCodes;
import com.yxx.rbac.model.RbacSubjectType;
import com.yxx.rbac.model.entity.RbacRole;
import com.yxx.rbac.service.RbacRoleService;
import com.yxx.rbac.service.RbacSubjectRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 管理员角色领域服务。
 *
 * <p>通用角色替换由 common-rbac 完成；“系统必须保留一个启用的超级管理员”属于管理端
 * 领域规则，因此保留在 admin，而不是污染公共 RBAC 模块。</p>
 */
@Service
@RequiredArgsConstructor
public class AdminUserRoleServiceImpl implements AdminUserRoleService {

    private final RbacRoleService roleService;
    private final RbacSubjectRoleService subjectRoleService;
    private final AdminUserMapper adminUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceRoles(Long userId, Collection<Integer> roleIds) {
        // 所有管理员角色替换共享同一数据库锁，避免并发移除最后两名超级管理员的角色。
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, adminUserMapper.lockSuperAdminGuard() != null);
        AdminUser targetUser = adminUserMapper.selectById(userId);
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST, targetUser != null);
        List<Integer> distinctRoleIds = roleIds == null
                ? List.of()
                : roleIds.stream().distinct().toList();

        RbacRole superAdminRole = roleService.findByCode(
                RbacScope.ADMIN, RbacSecurityCodes.ROLE_ADMIN_SUPER_ADMIN).orElse(null);
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, roleService.isCanonicalSuperRole(superAdminRole));
        boolean currentlySuperAdmin = subjectRoleService.hasRole(
                RbacSubjectType.ADMIN_USER.code(), userId, superAdminRole.getId());
        boolean keepsSuperAdminRole = distinctRoleIds.contains(superAdminRole.getId());
        if (currentlySuperAdmin && Boolean.TRUE.equals(targetUser.getStatus())
                && !keepsSuperAdminRole) {
            ApiAssert.isTrue(ApiCode.LAST_SUPER_ADMIN,
                    adminUserMapper.countActiveUsersByRoleCode(
                            RbacSecurityCodes.ROLE_ADMIN_SUPER_ADMIN) > 1);
        }

        subjectRoleService.replaceRoles(
                RbacSubjectType.ADMIN_USER.code(), userId, distinctRoleIds);
    }

}
