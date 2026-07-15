package com.yxx.business.service;

import java.util.Collection;
import java.util.List;

/** 用户端角色权限关联服务。 */
public interface RolePermissionService {

    /**
     * 替换角色权限，并注销拥有该角色的全部用户会话。
     *
     * @param roleId        角色主键
     * @param permissionIds 新权限主键集合
     */
    void replacePermissions(Integer roleId, Collection<Integer> permissionIds);

    /** 批量查询角色拥有的权限主键。 */
    List<Integer> listPermissionIdsByRoleIds(Collection<Integer> roleIds);
}
