package com.yxx.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.business.mapper.RoleMapper;
import com.yxx.business.model.entity.Role;
import com.yxx.business.service.RoleService;
import org.springframework.stereotype.Service;

/**
 * @author yxx
 * @since 2023-05-17 09:59
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
}
