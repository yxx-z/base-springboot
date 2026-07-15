package com.yxx.common.core.model;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 操作日志表
 *
 * @author yxx
 * @since 2022-07-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("operate_admin_log")
public class OperateAdminLog extends Convert {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 日志类型 1-正常日志 2-异常日志
     */
    private Integer type;

    /**
     * 操作模块
     */
    private String module;

    /**
     * 日志标题
     */
    private String title;

    /**
     * 操作IP
     */
    private String ip;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 请求URI
     */
    private String requestUri;

    /**
     * 操作方式
     */
    private String method;

    /**
     * 操作提交的数据
     */
    private String params;

    /**
     * tLog中的traceId
     */
    private String traceId;

    /**
     * tLog中的spanId
     */
    private String spanId;

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
    @TableField(fill = FieldFill.INSERT)
    private Long createUid;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Boolean isDelete;

}
