package com.yxx.business.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;


/**
 * 用户端后端权限资源。
 *
 * <p>菜单负责前端导航，权限负责后端授权。按钮不再作为独立权限模型，前端按钮根据权限编码
 * 决定是否显示。</p>
 */
@Data
public class Permission {

    /** 权限主键。 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 权限编码，例如 user:profile:read。 */
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
