package com.yxx.admin.model.response;

import lombok.Data;

import java.util.List;

/** 当前管理员可见的前端导航菜单。 */
@Data
public class AdminMenuRes {

    /** 菜单唯一编码。 */
    private String code;

    /** 菜单名称。 */
    private String name;

    /** 前端路由路径。 */
    private String path;

    /** 前端组件标识。 */
    private String component;

    /** 菜单图标。 */
    private String icon;

    /** 子菜单。 */
    private List<AdminMenuRes> children;
}
