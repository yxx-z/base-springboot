package com.yxx.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.admin.mapper.AdminPermissionMapper;
import com.yxx.admin.model.entity.AdminPermission;
import com.yxx.admin.service.AdminPermissionService;
import org.springframework.stereotype.Service;

/** 管理端权限服务实现。 */
@Service
public class AdminPermissionServiceImpl extends ServiceImpl<AdminPermissionMapper, AdminPermission>
        implements AdminPermissionService {
}
