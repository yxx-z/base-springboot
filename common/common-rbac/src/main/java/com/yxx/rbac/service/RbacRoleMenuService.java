package com.yxx.rbac.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.utils.ApiAssert;
import com.yxx.common.utils.NavigationTreeBuilder;
import com.yxx.rbac.mapper.RbacRoleMenuMapper;
import com.yxx.rbac.model.RbacMenuNode;
import com.yxx.rbac.model.RbacScope;
import com.yxx.rbac.model.entity.RbacMenu;
import com.yxx.rbac.model.entity.RbacRole;
import com.yxx.rbac.model.entity.RbacRoleMenu;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 统一角色菜单服务，菜单读取和写入都按权限域隔离。 */
@Service
@RequiredArgsConstructor
public class RbacRoleMenuService {

    private final RbacRoleMenuMapper roleMenuMapper;
    private final RbacRoleService roleService;
    private final RbacMenuService menuService;

    /** 根据当前角色构建指定权限域内的导航树。 */
    public List<RbacMenuNode> currentMenuTree(
            RbacScope scope, Collection<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }
        List<RbacRole> roles = roleService.findByIds(
                scope, roleService.findIdsByCodes(scope, roleCodes));
        if (roles.isEmpty()) {
            return List.of();
        }

        List<RbacMenu> enabledMenus = menuService.findEnabledMenus(scope);
        boolean hasSuperRole = roles.stream().anyMatch(roleService::isCanonicalSuperRole);
        Set<Integer> selectedMenuIds = hasSuperRole
                ? enabledMenus.stream().map(RbacMenu::getId)
                        .collect(Collectors.toCollection(HashSet::new))
                : roleMenuMapper.selectList(new LambdaQueryWrapper<RbacRoleMenu>()
                        .eq(RbacRoleMenu::getScope, scope.code())
                        .in(RbacRoleMenu::getRoleId,
                                roles.stream().map(RbacRole::getId).toList()))
                        .stream()
                        .map(RbacRoleMenu::getMenuId)
                        .collect(Collectors.toCollection(HashSet::new));
        if (selectedMenuIds.isEmpty()) {
            return List.of();
        }

        return NavigationTreeBuilder.build(
                enabledMenus, selectedMenuIds,
                RbacMenu::getId, RbacMenu::getParentId,
                menu -> Boolean.TRUE.equals(menu.getVisible()),
                menu -> menu.getSort() == null ? 0 : menu.getSort(),
                this::toResponse);
    }

    /** 替换角色菜单；角色和菜单必须属于同一权限域。 */
    @Transactional(rollbackFor = Exception.class)
    public void replaceMenus(Integer roleId, Collection<Integer> menuIds) {
        RbacRole role = roleService.findById(roleId).orElse(null);
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID, role != null);
        ApiAssert.isTrue(ApiCode.BUILT_IN_ROLE_IMMUTABLE,
                !Boolean.TRUE.equals(role.getBuiltIn()));

        RbacScope scope = RbacScope.fromCode(role.getScope());
        Set<Integer> distinctMenuIds = menuIds == null
                ? Set.of()
                : new HashSet<>(menuIds);
        ApiAssert.isTrue(ApiCode.RBAC_SCOPE_MISMATCH,
                menuService.findEnabledByIds(scope, distinctMenuIds).size()
                        == distinctMenuIds.size());

        roleMenuMapper.delete(new LambdaQueryWrapper<RbacRoleMenu>()
                .eq(RbacRoleMenu::getRoleId, roleId));
        if (!distinctMenuIds.isEmpty()) {
            List<RbacRoleMenu> relations = distinctMenuIds.stream().map(menuId -> {
                RbacRoleMenu relation = new RbacRoleMenu();
                relation.setScope(scope.code());
                relation.setRoleId(roleId);
                relation.setMenuId(menuId);
                return relation;
            }).toList();
            int inserted = relations.stream().mapToInt(roleMenuMapper::insert).sum();
            ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, inserted == relations.size());
        }
    }

    /** 批量按角色分组菜单主键，供配置列表避免 N+1 查询。 */
    public Map<Integer, List<Integer>> mapMenuIdsByRoleIds(
            RbacScope scope, Collection<Integer> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, List<Integer>> result = new LinkedHashMap<>();
        roleMenuMapper.selectList(new LambdaQueryWrapper<RbacRoleMenu>()
                .eq(RbacRoleMenu::getScope, scope.code())
                .in(RbacRoleMenu::getRoleId, roleIds)).forEach(relation ->
                result.computeIfAbsent(relation.getRoleId(), ignored ->
                        new java.util.ArrayList<>()).add(relation.getMenuId()));
        return result;
    }

    private RbacMenuNode toResponse(RbacMenu menu, List<RbacMenuNode> children) {
        RbacMenuNode response = new RbacMenuNode();
        response.setCode(menu.getMenuCode());
        response.setName(menu.getMenuName());
        response.setPath(menu.getPath());
        response.setComponent(menu.getComponent());
        response.setIcon(menu.getIcon());
        response.setChildren(children);
        return response;
    }
}
