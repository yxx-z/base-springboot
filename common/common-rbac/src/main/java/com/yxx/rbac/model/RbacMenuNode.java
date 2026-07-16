package com.yxx.rbac.model;

import lombok.Data;

import java.util.List;

/** 前端导航菜单节点，仅暴露路由渲染所需字段。 */
@Data
public class RbacMenuNode {

    /** 跨环境稳定的菜单编码。 */
    private String code;

    /** 菜单显示名称。 */
    private String name;

    /** 前端路由路径。 */
    private String path;

    /** 前端组件标识。 */
    private String component;

    /** 菜单图标。 */
    private String icon;

    /** 已按排序值组织的子菜单。 */
    private List<RbacMenuNode> children;
}
