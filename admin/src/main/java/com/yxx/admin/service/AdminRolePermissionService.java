package com.yxx.admin.service;

import java.util.Collection;
import java.util.List;

/** 管理端角色权限关联服务。 */
public interface AdminRolePermissionService {

    /**
     * 替换管理端角色权限，并注销拥有该角色的管理员会话。
     *
     * @param roleId        管理端角色主键
     * @param permissionIds 新权限主键集合
     */
    void replacePermissions(Integer roleId, Collection<Integer> permissionIds);

    /** 批量查询管理端角色拥有的权限主键。 */
    List<Integer> listPermissionIdsByRoleIds(Collection<Integer> roleIds);
}
