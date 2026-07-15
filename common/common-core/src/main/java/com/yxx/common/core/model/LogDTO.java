package com.yxx.common.core.model;

import cn.hutool.core.convert.Convert;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author yxx
 * @since 2022/7/26 11:30
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class LogDTO extends Convert {
    private static final long serialVersionUID = -2704193808014207916L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 操作模块
     */
    private String module;

    /**
     * 日志类型 1-正常日志 2-异常日志
     */
    private Integer type;

    /**
     * 日志标题
     */
    private String title;

    /**
     * 操作IP
     */
    private String ip;

    /**
     * ip归属地
     */
    private String ipHomePlace;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 请求URI
     */
    private String requestUri;

    private String traceId;

    private String spanId;

    /**
     * 操作方式
     */
    private String method;

    /**
     * 操作提交的数据
     */
    private String params;

    /**
     * 执行时间
     */
    private Long time;

    /**
     * 异常信息
     */
    private String exception;

    /**
     * 创建人
     */
    private Long createUid;
}
