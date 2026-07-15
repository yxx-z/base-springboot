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
import com.yxx.business.model.response.MenuRes;
import com.yxx.common.utils.TreeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    public List<MenuRes> currentMenuTree(Collection<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }
        List<Integer> roleIds = roleService.list(new LambdaQueryWrapper<Role>()
                        .in(Role::getCode, roleCodes))
                .stream()
                .map(Role::getId)
                .toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }

        Set<Integer> selectedMenuIds = list(new LambdaQueryWrapper<RoleMenu>()
                        .in(RoleMenu::getRoleId, roleIds))
                .stream()
                .map(RoleMenu::getMenuId)
                .collect(Collectors.toCollection(HashSet::new));
        if (selectedMenuIds.isEmpty()) {
            return List.of();
        }

        // 先加载全部有效菜单，再补齐被授权菜单的所有祖先，避免子菜单因父节点未直接授权而丢失。
        List<Menu> availableMenus = menuService.list(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getStatus, Boolean.TRUE)
                .eq(Menu::getVisible, Boolean.TRUE));
        Map<Integer, Menu> menuIndex = new HashMap<>();
        availableMenus.forEach(menu -> menuIndex.put(menu.getId(), menu));
        Set<Integer> visibleIds = new HashSet<>();
        for (Integer menuId : selectedMenuIds) {
            addMenuAndAncestors(menuId, menuIndex, visibleIds);
        }

        List<Menu> visibleMenus = availableMenus.stream()
                .filter(menu -> visibleIds.contains(menu.getId()))
                .toList();
        List<Menu> tree = TreeUtil.buildAscTree(
                visibleMenus, Menu::getParentId, Menu::getId, Menu::setChildren, null, Menu::getSort);
        return tree.stream().map(this::toResponse).toList();
    }

    private void addMenuAndAncestors(Integer menuId, Map<Integer, Menu> menuIndex, Set<Integer> visibleIds) {
        Integer currentId = menuId;
        Set<Integer> path = new HashSet<>();
        while (currentId != null && path.add(currentId)) {
            Menu menu = menuIndex.get(currentId);
            if (menu == null) {
                return;
            }
            visibleIds.add(currentId);
            currentId = menu.getParentId();
        }
        if (currentId != null) {
            throw new IllegalStateException("菜单数据存在循环父子关系，menuId=" + menuId);
        }
    }

    private MenuRes toResponse(Menu menu) {
        MenuRes response = new MenuRes();
        response.setCode(menu.getMenuCode());
        response.setName(menu.getMenuName());
        response.setPath(menu.getPath());
        response.setComponent(menu.getComponent());
        response.setIcon(menu.getIcon());
        response.setChildren(menu.getChildren() == null
                ? List.of()
                : menu.getChildren().stream().map(this::toResponse).toList());
        return response;
    }
}
