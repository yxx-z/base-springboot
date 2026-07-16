package com.yxx.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.business.mapper.PermissionMapper;
import com.yxx.business.model.entity.Permission;
import com.yxx.business.service.PermissionService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/** 用户端权限服务实现。 */
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission>
        implements PermissionService {

    @Override
    public List<Permission> findActiveByIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            // 空目标集合无需访问数据库，也避免构造非法 IN 条件。
            return List.of();
        }
        // 只返回启用权限，停用权限即使仍有历史关联也不会进入授权快照。
        return list(new LambdaQueryWrapper<Permission>()
                .in(Permission::getId, ids)
                .eq(Permission::getStatus, Boolean.TRUE));
    }
}
