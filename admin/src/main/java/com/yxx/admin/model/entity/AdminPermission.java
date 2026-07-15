package com.yxx.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;

/** 管理端后端权限资源。 */
@Data
public class AdminPermission implements Serializable {

    /** 权限主键。 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 权限编码。 */
    private String code;

    /** 权限名称。 */
    private String name;

    /** 资源类型，例如 api、operation、data。 */
    private String resourceType;

    /** 权限说明。 */
    private String description;

    /** 是否启用。 */
    private Boolean status;

    /** 逻辑删除标记。 */
    @TableLogic
    private Boolean isDelete;
}
