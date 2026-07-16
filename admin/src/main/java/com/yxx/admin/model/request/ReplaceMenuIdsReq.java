package com.yxx.admin.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/** 以最终集合语义替换角色菜单的请求。 */
public record ReplaceMenuIdsReq(
        @NotNull(message = "菜单集合不能为空")
        List<@NotNull(message = "菜单主键不能为空")
             @Positive(message = "菜单主键必须为正数") Integer> menuIds) {
}
