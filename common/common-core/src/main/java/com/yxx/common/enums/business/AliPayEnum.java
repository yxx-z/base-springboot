package com.yxx.common.enums.business;

import lombok.Getter;

/**
 * 支付宝支付公共错误码
 *
 * @author yxx
 * @since 2023-09-14 15:31
 */
@Getter
public enum AliPayEnum {
    /**
     * 接口调用成功
     */
    SUCCESS("10000", "接口调用成功"),

    /**
     * 服务不可用
     */
    UN_AVAILABLE("20000", "服务不可用"),

    /**
     * 授权权限不足
     */
    PERMISSION_DENIED("20001","授权权限不足"),

    /**
     * 缺少必选参数
     */
    MISSING_PARAMETER("40001", "缺少必选参数"),

    /**
     * 非法的参数
     */
    ILLEGAL_PARAMETER("40002", "非法的参数"),

    /**
     * 条件异常
     */
    INSUFFICIENT_CONDITIONS("40003", "条件异常"),

    /**
     * 业务处理失败
     */
    BUSINESS_FAILURE("40004", "业务处理失败"),

    /**
     * 调用频次超限
     */
    CALL_LIMITED("40005", "调用频次超限"),

    /**
     * 权限不足
     */
    ISV_PERMISSION_DENIED("40006", "权限不足");

    private final String code;
    private final String message;

    AliPayEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

}
