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
import com.yxx.admin.model.response.AdminMenuRes;
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
public class AdminRoleMenuServiceImpl extends ServiceImpl<AdminRoleMenuMapper, AdminRoleMenu> implements AdminRoleMenuService {
    private final AdminRoleService adminRoleService;
    private final AdminMenuService adminMenuService;

    @Override
    public List<AdminMenuRes> currentMenuTree(Collection<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }
        List<Integer> roleIds = adminRoleService.list(new LambdaQueryWrapper<AdminRole>()
                        .in(AdminRole::getCode, roleCodes))
                .stream()
                .map(AdminRole::getId)
                .toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }

        Set<Integer> selectedMenuIds = list(new LambdaQueryWrapper<AdminRoleMenu>()
                        .in(AdminRoleMenu::getRoleId, roleIds))
                .stream()
                .map(AdminRoleMenu::getMenuId)
                .collect(Collectors.toCollection(HashSet::new));
        if (selectedMenuIds.isEmpty()) {
            return List.of();
        }

        List<AdminMenu> availableMenus = adminMenuService.list(new LambdaQueryWrapper<AdminMenu>()
                .eq(AdminMenu::getStatus, Boolean.TRUE)
                .eq(AdminMenu::getVisible, Boolean.TRUE));
        Map<Integer, AdminMenu> menuIndex = new HashMap<>();
        availableMenus.forEach(menu -> menuIndex.put(menu.getId(), menu));
        Set<Integer> visibleIds = new HashSet<>();
        for (Integer menuId : selectedMenuIds) {
            addMenuAndAncestors(menuId, menuIndex, visibleIds);
        }

        List<AdminMenu> visibleMenus = availableMenus.stream()
                .filter(menu -> visibleIds.contains(menu.getId()))
                .toList();
        List<AdminMenu> tree = TreeUtil.buildAscTree(
                visibleMenus, AdminMenu::getParentId, AdminMenu::getId,
                AdminMenu::setChildren, null, AdminMenu::getSort);
        return tree.stream().map(this::toResponse).toList();
    }

    private void addMenuAndAncestors(Integer menuId,
                                     Map<Integer, AdminMenu> menuIndex,
                                     Set<Integer> visibleIds) {
        Integer currentId = menuId;
        Set<Integer> path = new HashSet<>();
        while (currentId != null && path.add(currentId)) {
            AdminMenu menu = menuIndex.get(currentId);
            if (menu == null) {
                return;
            }
            visibleIds.add(currentId);
            currentId = menu.getParentId();
        }
        if (currentId != null) {
            throw new IllegalStateException("管理端菜单数据存在循环父子关系，menuId=" + menuId);
        }
    }

    private AdminMenuRes toResponse(AdminMenu menu) {
        AdminMenuRes response = new AdminMenuRes();
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
