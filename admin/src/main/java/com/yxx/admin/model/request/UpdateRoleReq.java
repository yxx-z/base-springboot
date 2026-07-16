package com.yxx.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 修改普通角色请求；稳定角色编码不允许直接修改。 */
public record UpdateRoleReq(
        @NotBlank(message = "角色名称不能为空") @Size(max = 50) String name,
        @Size(max = 255) String remark) {
}
