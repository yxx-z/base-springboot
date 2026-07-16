package com.yxx.admin.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/** 以最终集合语义替换角色权限的请求。 */
public record ReplacePermissionIdsReq(
        @NotNull(message = "权限集合不能为空")
        List<@NotNull(message = "权限主键不能为空")
             @Positive(message = "权限主键必须为正数") Integer> permissionIds) {
}
