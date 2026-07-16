package com.yxx.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.business.mapper.RoleMenuMapper;
import com.yxx.business.model.entity.Menu;
import com.yxx.business.model.entity.RoleMenu;
import com.yxx.business.service.MenuService;
import com.yxx.business.service.RoleMenuService;
import com.yxx.business.service.RoleService;
import com.yxx.business.model.response.MenuRes;
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
public class RoleMenuServiceImpl extends ServiceImpl<RoleMenuMapper, RoleMenu> implements RoleMenuService {
    private final RoleService roleService;
    private final MenuService menuService;

    @Override
    public List<MenuRes> currentMenuTree(Collection<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            // 无角色主体没有可见导航，返回空集合而不是 null，简化前端处理。
            return List.of();
        }
        // 角色编码先转换为本模块数据库 ID，未知角色不会参与菜单查询。
        List<Integer> roleIds = roleService.findIdsByCodes(roleCodes);
        if (roleIds.isEmpty()) {
            return List.of();
        }

        // 多角色菜单取并集，Set 同时消除重复授权关系。
        Set<Integer> selectedMenuIds = list(new LambdaQueryWrapper<RoleMenu>()
                        .in(RoleMenu::getRoleId, roleIds))
                .stream()
                .map(RoleMenu::getMenuId)
                .collect(Collectors.toCollection(HashSet::new));
        if (selectedMenuIds.isEmpty()) {
            return List.of();
        }

        // 树构建器会补齐选中菜单的祖先容器，并处理隐藏父级和异常循环数据。
        return NavigationTreeBuilder.build(
                menuService.findEnabledMenus(), selectedMenuIds,
                Menu::getId, Menu::getParentId,
                menu -> Boolean.TRUE.equals(menu.getVisible()),
                menu -> menu.getSort() == null ? 0 : menu.getSort(),
                this::toResponse);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceMenus(Integer roleId, Collection<Integer> menuIds) {
        // 先验证角色和全部启用菜单，确保删除旧关系之前输入已完整合法。
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID, roleService.findByIds(List.of(roleId)).size() == 1);
        Set<Integer> distinctMenuIds = menuIds == null
                ? Set.of()
                : new HashSet<>(menuIds);
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID,
                menuService.findEnabledByIds(distinctMenuIds).size() == distinctMenuIds.size());

        // 关联采用“删除后重建”表达最终集合，逻辑简单且天然支持撤销全部菜单。
        remove(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));
        if (!distinctMenuIds.isEmpty()) {
            List<RoleMenu> relations = distinctMenuIds.stream().map(menuId -> {
                RoleMenu relation = new RoleMenu();
                relation.setRoleId(roleId);
                relation.setMenuId(menuId);
                return relation;
            }).toList();
            ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, saveBatch(relations));
        }
    }

    private MenuRes toResponse(Menu menu, List<MenuRes> children) {
        // 只向前端暴露导航所需字段，数据库主键、状态和审计字段不进入路由模型。
        MenuRes response = new MenuRes();
        response.setCode(menu.getMenuCode());
        response.setName(menu.getMenuName());
        response.setPath(menu.getPath());
        response.setComponent(menu.getComponent());
        response.setIcon(menu.getIcon());
        response.setChildren(children);
        return response;
    }
}
