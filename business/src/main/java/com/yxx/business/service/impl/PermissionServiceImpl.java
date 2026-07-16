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
            return List.of();
        }
        return list(new LambdaQueryWrapper<Permission>()
                .in(Permission::getId, ids)
                .eq(Permission::getStatus, Boolean.TRUE));
    }
}
