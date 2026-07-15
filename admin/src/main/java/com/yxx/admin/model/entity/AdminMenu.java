package com.yxx.admin.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author yxx
 * @since 2023-05-18 14:46
 */
@Data
public class AdminMenu implements Serializable {
    /**
     * 主键
     */

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 父id
     */
    private Integer parentId;

    /**
     * 菜单标识
     */
    private String menuCode;

    /**
     * 菜单名称
     */
    private String menuName;

    /** 前端路由路径。 */
    private String path;

    /** 前端组件标识。 */
    private String component;

    /** 菜单图标。 */
    private String icon;

    /** 同级菜单排序值，数值越小越靠前。 */
    private Integer sort;

    /** 是否在导航中显示。 */
    private Boolean visible;

    /** 菜单状态：1-启用，0-停用。 */
    private Boolean status;

    /**
     * 是否删除: 0- 否; 1- 是
     */
    @TableLogic
    private Boolean isDelete;

    /**
     * 子菜单集合
     */
    @TableField(exist = false)
    private List<AdminMenu> children;
}
