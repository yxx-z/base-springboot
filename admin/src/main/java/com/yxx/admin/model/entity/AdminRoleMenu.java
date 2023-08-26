package com.yxx.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
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
    private Integer id;

    /**
     * 角色id
     */
    private Integer roleId;

    /**
     * 菜单id
     */
    private Integer menuId;

    /**
     * 是否删除：0-未删除；1-已删除
     */
    @TableLogic
    private Boolean isDelete;
}
