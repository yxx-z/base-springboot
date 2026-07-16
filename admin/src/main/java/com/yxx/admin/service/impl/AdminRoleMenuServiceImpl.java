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
            // 无角色管理员不展示任何管理导航，使用空集合保持响应结构稳定。
            return List.of();
        }
        List<Integer> roleIds = adminRoleService.findIdsByCodes(roleCodes);
        if (roleIds.isEmpty()) {
            return List.of();
        }

        // 一次加载全部启用菜单，后续授权筛选和祖先补齐都基于同一数据快照。
        List<AdminMenu> enabledMenus = adminMenuService.findEnabledMenus();
        // 超级管理员天然拥有全部菜单；其他角色取关联菜单并集。
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

        // 通用树构建器负责排序、祖先容器保留、隐藏节点处理及循环数据保护。
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
        // 先加载角色实体，既校验存在性，也用于识别不可修改的内置超级角色。
        AdminRole role = adminRoleService.findByIds(List.of(roleId)).stream().findFirst().orElse(null);
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID, role != null);
        // 超级角色的全菜单能力由代码不变量提供，不允许维护可漂移的关联数据。
        ApiAssert.isTrue(ApiCode.BUILT_IN_ROLE_IMMUTABLE,
                !AdminSecurityCodes.ROLE_SUPER_ADMIN.equals(role.getCode()));
        // 所有目标菜单必须存在且启用，验证完成后才删除旧关系。
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
        // 导航响应仅包含前端路由字段，不泄露数据库状态和审计信息。
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
