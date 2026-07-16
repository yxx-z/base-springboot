package com.yxx.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 修改前端导航菜单请求；权限域和稳定菜单编码不允许直接修改。 */
public record UpdateMenuReq(
        @Positive(message = "父菜单主键必须为正数") Integer parentId,
        @NotBlank(message = "菜单名称不能为空") @Size(max = 50) String menuName,
        @Size(max = 255) String path,
        @Size(max = 255) String component,
        @Size(max = 100) String icon,
        @NotNull(message = "菜单排序不能为空") Integer sort,
        @NotNull(message = "菜单显示状态不能为空") Boolean visible,
        @NotNull(message = "菜单启用状态不能为空") Boolean enabled) {
}
