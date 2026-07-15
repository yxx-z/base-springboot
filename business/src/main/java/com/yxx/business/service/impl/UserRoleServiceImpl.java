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
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {
    private final RoleService roleService;

    @Override
    public List<String> loginUserRoleManage(User user) {
        // 初始化返回角色code集合
        List<String> roleList = new LinkedList<>();
        // 根据用户id 获取该用户的角色集合
        List<UserRole> userRoleList = list(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
        // 如果没有角色不让登录，请取消下面一行代码注释
        // ApiAssert.isTrue(ApiCode.USER_NOT_ROLE, !userRoleList.isEmpty());

        // 遍历用户角色集合
        userRoleList.forEach(userRole -> {
            // 根据用户的角色id 获取角色详情
            Role role = roleService.getOne(new LambdaQueryWrapper<Role>().eq(Role::getId, userRole.getRoleId()));

            // 如果没有角色不让登录，请取消下面一行代码注释
            // ApiAssert.isTrue(ApiCode.USER_NOT_ROLE, ObjectUtil.isNull(role));

            // 如果角色信息不为空
            if (ObjectUtil.isNotNull(role)) {
                // 将角色code添加到返回值集合中
                roleList.add(role.getCode());
            }
        });

        // 返回角色code集合
        return roleList;
    }

    @Override
    public Boolean setDefaultRole(User user) {
        // 根据角色code 获取角色详情
        Role role = roleService.getOne(
                new LambdaQueryWrapper<Role>().eq(Role::getCode, RoleEnum.USER.getCode()));
        // 初始化用户角色实体类
        UserRole userRole = new UserRole();
        // 设置用户id
        userRole.setUserId(user.getId());
        // 设置角色id
        userRole.setRoleId(role.getId());
        // 保存
        return save(userRole);
    }
}
