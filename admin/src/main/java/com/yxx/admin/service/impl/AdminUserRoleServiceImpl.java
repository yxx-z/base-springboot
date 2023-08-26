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
import com.yxx.common.enums.business.RoleEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

/**
 * @author yxx
 * @since 2023-05-17 10:01
 */
@Service
@RequiredArgsConstructor
public class AdminUserRoleServiceImpl extends ServiceImpl<AdminUserRoleMapper, AdminUserRole> implements AdminUserRoleService {
    private final AdminRoleService adminRoleService;

    @Override
    public List<String> loginUserRoleManage(AdminUser user) {
        // 初始化返回角色code集合
        List<String> roleList = new LinkedList<>();
        // 根据用户id 获取该用户的角色集合
        List<AdminUserRole> adminUserRoleList = list(new LambdaQueryWrapper<AdminUserRole>().eq(AdminUserRole::getUserId, user.getId()));
        // 如果没有角色不让登录，请取消下面一行代码注释
        // ApiAssert.isTrue(ApiCode.USER_NOT_ROLE, !userRoleList.isEmpty());

        // 遍历用户角色集合
        adminUserRoleList.forEach(adminUserRole -> {
            // 根据用户的角色id 获取角色详情
            AdminRole adminRole = adminRoleService.getOne(new LambdaQueryWrapper<AdminRole>().eq(AdminRole::getId, adminUserRole.getRoleId()));

            // 如果没有角色不让登录，请取消下面一行代码注释
            // ApiAssert.isTrue(ApiCode.USER_NOT_ROLE, ObjectUtil.isNull(role));

            // 如果角色信息不为空
            if (ObjectUtil.isNotNull(adminRole)) {
                // 将角色code添加到返回值集合中
                roleList.add(adminRole.getCode());
            }
        });

        // 返回角色code集合
        return roleList;
    }

    @Override
    public Boolean setDefaultRole(AdminUser user) {
        // 根据角色code 获取角色详情
        AdminRole adminRole = adminRoleService.getOne(
                new LambdaQueryWrapper<AdminRole>().eq(AdminRole::getCode, RoleEnum.USER.getCode()));
        // 初始化用户角色实体类
        AdminUserRole adminUserRole = new AdminUserRole();
        // 设置用户id
        adminUserRole.setUserId(user.getId());
        // 设置角色id
        adminUserRole.setRoleId(adminRole.getId());
        // 保存
        return save(adminUserRole);
    }
}
