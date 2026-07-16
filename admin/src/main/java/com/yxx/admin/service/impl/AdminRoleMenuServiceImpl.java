package com.yxx.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.admin.model.entity.AdminRoleMenu;
import com.yxx.admin.model.entity.AdminRole;
import com.yxx.admin.mapper.AdminRoleMenuMapper;
import com.yxx.admin.model.entity.AdminMenu;
import com.yxx.admin.service.AdminMenuService;
import com.yxx.admin.service.AdminRoleMenuService;
import com.yxx.admin.service.AdminRoleService;
import com.yxx.admin.security.AdminSecurityCodes;
import com.yxx.admin.model.response.AdminMenuRes;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.utils.ApiAssert;
import com.yxx.common.utils.NavigationTreeBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

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
        List<Integer> roleIds = adminRoleService.findIdsByCodes(roleCodes);
        if (roleIds.isEmpty()) {
            return List.of();
        }

        List<AdminMenu> enabledMenus = adminMenuService.findEnabledMenus();
        Set<Integer> selectedMenuIds = roleCodes.contains(AdminSecurityCodes.ROLE_SUPER_ADMIN)
                ? enabledMenus.stream().map(AdminMenu::getId)
                        .collect(Collectors.toCollection(HashSet::new))
                : list(new LambdaQueryWrapper<AdminRoleMenu>()
                        .in(AdminRoleMenu::getRoleId, roleIds))
                        .stream()
                        .map(AdminRoleMenu::getMenuId)
                        .collect(Collectors.toCollection(HashSet::new));
        if (selectedMenuIds.isEmpty()) {
            return List.of();
        }

        return NavigationTreeBuilder.build(
                enabledMenus, selectedMenuIds,
                AdminMenu::getId, AdminMenu::getParentId,
                menu -> Boolean.TRUE.equals(menu.getVisible()),
                menu -> menu.getSort() == null ? 0 : menu.getSort(),
                this::toResponse);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceMenus(Integer roleId, Collection<Integer> menuIds) {
        AdminRole role = adminRoleService.findByIds(List.of(roleId)).stream().findFirst().orElse(null);
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID, role != null);
        ApiAssert.isTrue(ApiCode.BUILT_IN_ROLE_IMMUTABLE,
                !AdminSecurityCodes.ROLE_SUPER_ADMIN.equals(role.getCode()));
        Set<Integer> distinctMenuIds = menuIds == null
                ? Set.of()
                : new HashSet<>(menuIds);
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID,
                adminMenuService.findEnabledByIds(distinctMenuIds).size() == distinctMenuIds.size());

        remove(new LambdaQueryWrapper<AdminRoleMenu>()
                .eq(AdminRoleMenu::getRoleId, roleId));
        if (!distinctMenuIds.isEmpty()) {
            List<AdminRoleMenu> relations = distinctMenuIds.stream().map(menuId -> {
                AdminRoleMenu relation = new AdminRoleMenu();
                relation.setRoleId(roleId);
                relation.setMenuId(menuId);
                return relation;
            }).toList();
            ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, saveBatch(relations));
        }
    }

    private AdminMenuRes toResponse(AdminMenu menu, List<AdminMenuRes> children) {
        AdminMenuRes response = new AdminMenuRes();
        response.setCode(menu.getMenuCode());
        response.setName(menu.getMenuName());
        response.setPath(menu.getPath());
        response.setComponent(menu.getComponent());
        response.setIcon(menu.getIcon());
        response.setChildren(children);
        return response;
    }
}
