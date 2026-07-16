package com.yxx.admin.model.response;

import java.util.List;

/** 管理端角色配置视图。 */
public record RbacRoleRes(
        Integer id,
        String scope,
        String code,
        String name,
        String remark,
        Boolean builtIn,
        Boolean superRole,
        List<Integer> permissionIds,
        List<Integer> menuIds) {

    public RbacRoleRes {
        permissionIds = permissionIds == null ? List.of() : List.copyOf(permissionIds);
        menuIds = menuIds == null ? List.of() : List.copyOf(menuIds);
    }
}
