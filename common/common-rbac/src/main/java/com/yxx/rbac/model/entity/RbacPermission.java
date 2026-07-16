package com.yxx.rbac.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 统一 RBAC 后端权限资源。 */
@Data
@TableName("rbac_permission")
public class RbacPermission {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 权限所属的 admin 或 business 权限域。 */
    private String scope;

    /** 权限域内唯一的后端权限编码。 */
    private String code;

    private String name;

    /** 资源类型，例如 api、operation、data。 */
    private String resourceType;

    private String description;

    private Boolean status;

    @TableLogic
    private Boolean isDelete;
}
