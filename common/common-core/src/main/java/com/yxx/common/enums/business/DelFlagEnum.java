package com.yxx.common.enums.business;

import lombok.Getter;

/**
 * @author yxx
 * @since 2022-05-07 11:36
 */
@Getter
public enum DelFlagEnum {
    /**
     * 是否删除
     */
    YES(1, "删除"),
    NO(0, "未删除");

    private final Integer code;

    private final String message;

    DelFlagEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}
