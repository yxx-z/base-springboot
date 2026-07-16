package com.yxx.admin.model.response;

/** 管理端菜单资源配置视图。 */
public record RbacMenuRes(
        Integer id,
        String scope,
        Integer parentId,
        String code,
        String name,
        String path,
        String component,
        String icon,
        Integer sort,
        Boolean visible,
        Boolean status) {
}
