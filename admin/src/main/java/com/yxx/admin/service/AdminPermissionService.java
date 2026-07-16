package com.yxx.admin.service;

import com.yxx.admin.model.entity.AdminPermission;

import java.util.Collection;
import java.util.List;

/** 管理端权限服务。 */
public interface AdminPermissionService {

    List<AdminPermission> findActiveByIds(Collection<Integer> ids);
}
