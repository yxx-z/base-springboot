package com.yxx.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxx.admin.model.entity.AdminRolePermission;

import java.util.Collection;

/** 管理端角色权限关联服务。 */
public interface AdminRolePermissionService extends IService<AdminRolePermission> {

    /**
     * 替换管理端角色权限，并注销拥有该角色的管理员会话。
     *
     * @param roleId        管理端角色主键
     * @param permissionIds 新权限主键集合
     */
    void replacePermissions(Integer roleId, Collection<Integer> permissionIds);
}
