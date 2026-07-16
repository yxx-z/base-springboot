package com.yxx.rbac.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yxx.rbac.mapper.RbacMenuMapper;
import com.yxx.rbac.model.RbacScope;
import com.yxx.rbac.model.entity.RbacMenu;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;

/** 统一前端菜单查询服务。 */
@Service
@RequiredArgsConstructor
public class RbacMenuService {

    private final RbacMenuMapper menuMapper;

    /** 查询权限域内全部启用菜单，隐藏节点也参与祖先链计算。 */
    public List<RbacMenu> findEnabledMenus(RbacScope scope) {
        return menuMapper.selectList(new LambdaQueryWrapper<RbacMenu>()
                .eq(RbacMenu::getScope, scope.code())
                .eq(RbacMenu::getStatus, Boolean.TRUE));
    }

    /** 查询权限域内全部未删除菜单，供管理端同时维护启用和停用配置。 */
    public List<RbacMenu> listByScope(RbacScope scope) {
        return menuMapper.selectList(new LambdaQueryWrapper<RbacMenu>()
                .eq(RbacMenu::getScope, scope.code())
                .orderByAsc(RbacMenu::getSort)
                .orderByAsc(RbacMenu::getId));
    }

    /** 查询权限域内指定主键对应的启用菜单。 */
    public List<RbacMenu> findEnabledByIds(RbacScope scope, Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return menuMapper.selectList(new LambdaQueryWrapper<RbacMenu>()
                .eq(RbacMenu::getScope, scope.code())
                .eq(RbacMenu::getStatus, Boolean.TRUE)
                .in(RbacMenu::getId, ids));
    }
}
