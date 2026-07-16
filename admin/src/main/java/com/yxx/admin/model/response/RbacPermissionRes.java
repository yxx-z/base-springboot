package com.yxx.admin.model.response;

/** 管理端权限资源配置视图。 */
public record RbacPermissionRes(
        Integer id,
        String scope,
        String code,
        String name,
        String resourceType,
        String description,
        Boolean status) {
}
