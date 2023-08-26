package com.yxx.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.admin.model.entity.AdminRoleMenu;
import com.yxx.admin.mapper.AdminRoleMenuMapper;
import com.yxx.admin.model.entity.AdminMenu;
import com.yxx.admin.model.entity.AdminRole;
import com.yxx.admin.service.AdminMenuService;
import com.yxx.admin.service.AdminRoleMenuService;
import com.yxx.admin.service.AdminRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yxx
 * @since 2023-05-18 15:23
 */
@Service
@RequiredArgsConstructor
public class AdminRoleMenuServiceImpl extends ServiceImpl<AdminRoleMenuMapper, AdminRoleMenu> implements AdminRoleMenuService {
    private final AdminRoleService adminRoleService;
    private final AdminMenuService adminMenuService;

    @Override
    public List<String> loginUserMenu(List<String> roleCodeList) {
        // 如果角色code集合不为空
        if (!roleCodeList.isEmpty()) {
            // 根据角色code集合获取角色集合
            List<AdminRole> adminRoleList = adminRoleService.list(new LambdaQueryWrapper<AdminRole>().in(AdminRole::getCode, roleCodeList));
            // 根据角色集合 获取角色id集合
            List<Integer> roleIdList = adminRoleList.stream().map(AdminRole::getId).collect(Collectors.toList());
            // 根据角色id集合 获取 角色菜单 集合
            List<AdminRoleMenu> adminRoleMenuList = list(new LambdaQueryWrapper<AdminRoleMenu>().in(AdminRoleMenu::getRoleId, roleIdList));

            // 如果角色菜单集合不为空
            if (!adminRoleMenuList.isEmpty()) {
                // 根据角色菜单集合 获取菜单id集合
                List<Integer> menuIdList = adminRoleMenuList.stream().map(AdminRoleMenu::getMenuId).collect(Collectors.toList());
                // 根据菜单id集合 获取菜单集合
                List<AdminMenu> adminMenuList = adminMenuService.list(new LambdaQueryWrapper<AdminMenu>().in(AdminMenu::getId, menuIdList));
                // 根据菜单集合 获取菜单code集合 并返回
                return adminMenuList.stream().map(AdminMenu::getMenuCode).collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}
