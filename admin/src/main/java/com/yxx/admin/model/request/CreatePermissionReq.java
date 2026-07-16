package com.yxx.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 新增后端权限资源请求。 */
public record CreatePermissionReq(
        @NotBlank(message = "权限域不能为空") String scope,
        @NotBlank(message = "权限编码不能为空") @Size(max = 150) String code,
        @NotBlank(message = "权限名称不能为空") @Size(max = 100) String name,
        @NotBlank(message = "资源类型不能为空") @Size(max = 30) String resourceType,
        @Size(max = 255) String description,
        @NotNull(message = "权限状态不能为空") Boolean enabled) {
}
