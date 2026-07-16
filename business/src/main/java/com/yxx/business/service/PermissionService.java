package com.yxx.business.service;

import com.yxx.business.model.entity.Permission;

import java.util.Collection;
import java.util.List;

/** 用户端权限服务。 */
public interface PermissionService {

    /** 查询指定主键中当前启用的权限。 */
    List<Permission> findActiveByIds(Collection<Integer> ids);
}
