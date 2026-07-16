package com.yxx.rbac.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 统一 RBAC 前端导航菜单。 */
@Data
@TableName("rbac_menu")
public class RbacMenu {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 菜单所属的 admin 或 business 权限域。 */
    private String scope;

    private Integer parentId;

    private String menuCode;

    private String menuName;

    private String path;

    private String component;

    private String icon;

    private Integer sort;

    private Boolean visible;

    private Boolean status;

    @TableLogic
    private Boolean isDelete;
}
