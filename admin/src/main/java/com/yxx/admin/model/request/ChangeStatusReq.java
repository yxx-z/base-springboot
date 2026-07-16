package com.yxx.admin.model.request;

import jakarta.validation.constraints.NotNull;

/** 启用或停用账号请求。 */
public record ChangeStatusReq(@NotNull(message = "账号状态不能为空") Boolean enabled) {
}
