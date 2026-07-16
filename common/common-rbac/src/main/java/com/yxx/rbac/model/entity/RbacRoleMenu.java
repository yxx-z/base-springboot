package com.yxx.rbac.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 角色与前端菜单的关联关系。 */
@Data
@TableName("rbac_role_menu")
public class RbacRoleMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色与菜单共同所属的权限域。 */
    private String scope;

    private Integer roleId;

    private Integer menuId;
}
