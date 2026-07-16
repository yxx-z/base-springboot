package com.yxx.rbac.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yxx.rbac.mapper.RbacPermissionMapper;
import com.yxx.rbac.model.RbacScope;
import com.yxx.rbac.model.entity.RbacPermission;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;

/** 统一后端权限资源查询服务。 */
@Service
@RequiredArgsConstructor
public class RbacPermissionService {

    private final RbacPermissionMapper permissionMapper;

    /** 查询权限域内指定主键对应的启用权限。 */
    public List<RbacPermission> findActiveByIds(RbacScope scope, Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return permissionMapper.selectList(new LambdaQueryWrapper<RbacPermission>()
                .eq(RbacPermission::getScope, scope.code())
                .eq(RbacPermission::getStatus, Boolean.TRUE)
                .in(RbacPermission::getId, ids));
    }

    /** 查询指定权限域的全部有效权限资源。 */
    public List<RbacPermission> listByScope(RbacScope scope) {
        return permissionMapper.selectList(new LambdaQueryWrapper<RbacPermission>()
                .eq(RbacPermission::getScope, scope.code())
                .orderByAsc(RbacPermission::getId));
    }
}
