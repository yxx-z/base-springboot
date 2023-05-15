package com.yxx.common.core.page;

import lombok.Data;

import javax.validation.constraints.Min;

/**
 * 分页参数基本信息
 *
 * @author yxx
 * @since 2022-02-07 21:40
 */
@Data
public class BasePageRequest {
    /**
     * 页面大小
     */
    @Min(value = 1, message = "页面大小不合法")
    private Integer pageSize = 10;

    /**
     * 页码
     */
    @Min(value = 1, message = "页码不合法")
    private Integer page = 1;
}
