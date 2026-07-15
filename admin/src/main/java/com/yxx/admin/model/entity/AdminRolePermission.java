package com.yxx.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/** 管理端角色权限关联关系。 */
@Data
public class AdminRolePermission {

    /** 关联关系主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色主键。 */
    private Integer roleId;

    /** 权限主键。 */
    private Integer permissionId;

}
