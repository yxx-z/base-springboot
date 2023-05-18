package com.yxx.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.business.mapper.RoleMenuMapper;
import com.yxx.business.model.entity.Menu;
import com.yxx.business.model.entity.Role;
import com.yxx.business.model.entity.RoleMenu;
import com.yxx.business.service.MenuService;
import com.yxx.business.service.RoleMenuService;
import com.yxx.business.service.RoleService;
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
public class RoleMenuServiceImpl extends ServiceImpl<RoleMenuMapper, RoleMenu> implements RoleMenuService {
    private final RoleService roleService;
    private final MenuService menuService;

    @Override
    public List<String> loginUserMenu(List<String> roleCodeList) {
        // 如果角色code集合不为空
        if (!roleCodeList.isEmpty()) {
            // 根据角色code集合获取角色集合
            List<Role> roleList = roleService.list(new LambdaQueryWrapper<Role>().in(Role::getCode, roleCodeList));
            // 根据角色集合 获取角色id集合
            List<Integer> roleIdList = roleList.stream().map(Role::getId).collect(Collectors.toList());
            // 根据角色id集合 获取 角色菜单 集合
            List<RoleMenu> roleMenuList = list(new LambdaQueryWrapper<RoleMenu>().in(RoleMenu::getRoleId, roleIdList));

            // 如果角色菜单集合不为空
            if (!roleMenuList.isEmpty()) {
                // 根据角色菜单集合 获取菜单id集合
                List<Integer> menuIdList = roleMenuList.stream().map(RoleMenu::getMenuId).collect(Collectors.toList());
                // 根据菜单id集合 获取菜单集合
                List<Menu> menuList = menuService.list(new LambdaQueryWrapper<Menu>().in(Menu::getId, menuIdList));
                // 根据菜单集合 获取菜单code集合 并返回
                return menuList.stream().map(Menu::getMenuCode).collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}
