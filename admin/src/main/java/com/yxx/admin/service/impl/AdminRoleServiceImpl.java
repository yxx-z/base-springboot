package com.yxx.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.admin.mapper.AdminRoleMapper;
import com.yxx.admin.model.entity.AdminRole;
import com.yxx.admin.service.AdminRoleService;
import org.springframework.stereotype.Service;

/**
 * @author yxx
 * @since 2023-05-17 09:59
 */
@Service
public class AdminRoleServiceImpl extends ServiceImpl<AdminRoleMapper, AdminRole> implements AdminRoleService {
}
