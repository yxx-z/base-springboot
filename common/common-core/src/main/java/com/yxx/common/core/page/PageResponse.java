package com.yxx.common.core.page;

import java.util.List;

/**
 * 与持久化框架无关的统一分页响应。
 *
 * <p>Controller 只暴露稳定的接口契约，不直接返回 MyBatis-Plus 等基础设施类型，避免替换
 * ORM 或升级依赖时影响前端接口。</p>
 *
 * @param records 当前页数据
 * @param page 当前页码，从 1 开始
 * @param pageSize 每页数据量
 * @param total 总数据量
 * @param pages 总页数
 * @param <T> 数据类型
 */
public record PageResponse<T>(
        List<T> records,
        long page,
        long pageSize,
        long total,
        long pages) {

    /**
     * 创建不可变分页响应，并防止调用方继续修改 records。
     */
    public PageResponse {
        records = records == null ? List.of() : List.copyOf(records);
    }
}
