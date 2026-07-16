package com.yxx.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 新增普通角色请求；内置角色和超级角色只能由数据库迁移创建。 */
public record CreateRoleReq(
        @NotBlank(message = "权限域不能为空") String scope,
        @NotBlank(message = "角色编码不能为空") @Size(max = 100) String code,
        @NotBlank(message = "角色名称不能为空") @Size(max = 50) String name,
        @Size(max = 255) String remark) {
}
