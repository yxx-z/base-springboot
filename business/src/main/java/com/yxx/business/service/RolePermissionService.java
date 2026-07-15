package com.yxx.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxx.business.model.entity.RolePermission;

import java.util.Collection;

/** 用户端角色权限关联服务。 */
public interface RolePermissionService extends IService<RolePermission> {

    /**
     * 替换角色权限，并注销拥有该角色的全部用户会话。
     *
     * @param roleId        角色主键
     * @param permissionIds 新权限主键集合
     */
    void replacePermissions(Integer roleId, Collection<Integer> permissionIds);
}
