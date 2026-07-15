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
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {
    private final RoleService roleService;

    private final LoginSessionService loginSessionService;

    @Override
    public List<String> loginUserRoleManage(User user) {
        // 初始化返回角色code集合
        List<String> roleList = new LinkedList<>();
        // 根据用户id 获取该用户的角色集合
        List<UserRole> userRoleList = list(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
        // 如果没有角色不让登录，请取消下面一行代码注释
        // ApiAssert.isTrue(ApiCode.USER_NOT_ROLE, !userRoleList.isEmpty());

        // 一次批量加载全部角色，避免每个用户角色关系再执行一次数据库查询。
        List<Integer> roleIds = userRoleList.stream().map(UserRole::getRoleId).distinct().toList();
        if (!roleIds.isEmpty()) {
            roleService.listByIds(roleIds).stream()
                    .map(Role::getCode)
                    .distinct()
                    .forEach(roleList::add);
        }

        // 返回角色code集合
        return roleList;
    }

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
        loginSessionService.invalidateUser(userId);
    }
}
