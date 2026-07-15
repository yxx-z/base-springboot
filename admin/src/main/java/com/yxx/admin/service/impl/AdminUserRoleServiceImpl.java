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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Collection;
import com.yxx.security.context.LoginSessionService;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author yxx
 * @since 2023-05-17 10:01
 */
@Service
@RequiredArgsConstructor
public class AdminUserRoleServiceImpl extends ServiceImpl<AdminUserRoleMapper, AdminUserRole> implements AdminUserRoleService {
    private final AdminRoleService adminRoleService;

    private final LoginSessionService loginSessionService;

    @Override
    public List<String> loginUserRoleManage(AdminUser user) {
        // 初始化返回角色code集合
        List<String> roleList = new LinkedList<>();
        // 根据用户id 获取该用户的角色集合
        List<AdminUserRole> adminUserRoleList = list(new LambdaQueryWrapper<AdminUserRole>().eq(AdminUserRole::getUserId, user.getId()));
        // 如果没有角色不让登录，请取消下面一行代码注释
        // ApiAssert.isTrue(ApiCode.USER_NOT_ROLE, !userRoleList.isEmpty());

        // 遍历用户角色集合
        List<Integer> roleIds = adminUserRoleList.stream()
                .map(AdminUserRole::getRoleId)
                .distinct()
                .toList();
        if (!roleIds.isEmpty()) {
            adminRoleService.listByIds(roleIds).stream()
                    .map(AdminRole::getCode)
                    .distinct()
                    .forEach(roleList::add);
        }

        // 返回角色code集合
        return roleList;
    }

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
        loginSessionService.invalidateAdmin(userId);
    }
}
