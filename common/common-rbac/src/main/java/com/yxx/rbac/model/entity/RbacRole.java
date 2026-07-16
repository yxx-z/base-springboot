package com.yxx.rbac.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 统一 RBAC 角色；通过 scope 区分管理端角色和业务端角色。 */
@Data
@TableName("rbac_role")
public class RbacRole {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 角色所属权限域，取值为 admin 或 business。 */
    private String scope;

    /** 权限域内唯一、采用冒号分层的稳定角色编码。 */
    private String code;

    private String name;

    private String remark;

    /** 内置角色不可修改授权关系，防止破坏框架安全不变量。 */
    private Boolean builtIn;

    /** 超级角色直接获得权限通配符和本权限域全部有效菜单。 */
    private Boolean superRole;

    @TableLogic
    private Boolean isDelete;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
