package com.yxx.admin.model.entity;

import lombok.Data;

/**
 * @author yxx
 * @since 2023-05-18 15:18
 */
@Data
public class AdminRoleMenu {
    /**
     * 主键
     */
    private Long id;

    /**
     * 角色id
     */
    private Integer roleId;

    /**
     * 菜单id
     */
    private Integer menuId;

}
