package com.yxx.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.business.mapper.PermissionMapper;
import com.yxx.business.model.entity.Permission;
import com.yxx.business.service.PermissionService;
import org.springframework.stereotype.Service;

/** 用户端权限服务实现。 */
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission>
        implements PermissionService {
}
