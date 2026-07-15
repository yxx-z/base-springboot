package com.yxx.business.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author yxx
 * @since 2023-05-18 14:46
 */
@Data
public class Menu implements Serializable {
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

    /**
     * 是否删除: 0- 否; 1- 是
     */
    @TableLogic
    private Boolean isDelete;

    /**
     * 子菜单集合
     */
    @TableField(exist = false)
    private List<Menu> children;
}