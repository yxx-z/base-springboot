package com.yxx.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author yxx
 * @since 2022/7/26 11:20
 */
@Getter
@AllArgsConstructor
public enum LogTypeEnum {
    /**
     * 正常日志类型
     */
    NORMAL(1, "正常日志"),

    /**
     * 错误日志类型
     */
    ERROR(2, "错误日志");

    /**
     * 类型
     */
    private final Integer code;

    /**
     * 描述
     */
    private final String message;
}
