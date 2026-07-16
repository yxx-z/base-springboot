package com.yxx.rbac.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 登录主体与统一角色的关联关系。 */
@Data
@TableName("rbac_subject_role")
public class RbacSubjectRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 主体类型：user 表示业务用户，admin 表示管理端账号。 */
    private String subjectType;

    /** 对应 user 或 admin_user 表中的稳定主键。 */
    private Long subjectId;

    /** 冗余保存权限域，用于数据库复合外键和 CHECK 约束形成最后一道隔离防线。 */
    private String scope;

    private Integer roleId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
