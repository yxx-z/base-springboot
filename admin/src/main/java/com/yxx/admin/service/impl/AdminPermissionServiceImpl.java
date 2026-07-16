package com.yxx.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.admin.mapper.AdminPermissionMapper;
import com.yxx.admin.model.entity.AdminPermission;
import com.yxx.admin.service.AdminPermissionService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/** 管理端权限服务实现。 */
@Service
public class AdminPermissionServiceImpl extends ServiceImpl<AdminPermissionMapper, AdminPermission>
        implements AdminPermissionService {

    @Override
    public List<AdminPermission> findActiveByIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            // 空集合直接返回，供上层安全地执行数量一致性校验。
            return List.of();
        }
        // 历史关联指向停用权限时不返回，保证会话快照只包含当前有效授权。
        return list(new LambdaQueryWrapper<AdminPermission>()
                .in(AdminPermission::getId, ids)
                .eq(AdminPermission::getStatus, Boolean.TRUE));
    }
}
