package com.yxx.admin.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/** 以最终集合语义替换主体角色的请求。 */
public record ReplaceRoleIdsReq(
        @NotNull(message = "角色集合不能为空")
        List<@NotNull(message = "角色主键不能为空")
             @Positive(message = "角色主键必须为正数") Integer> roleIds) {
}
