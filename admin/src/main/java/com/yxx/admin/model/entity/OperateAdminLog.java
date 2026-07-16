package com.yxx.admin.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 管理端操作审计日志实体。 */
@Data
@TableName("operate_admin_log")
public class OperateAdminLog {

    /** 审计日志主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 操作管理员主键；匿名操作为空。 */
    private Long userId;

    /** 事件发生时的主体类型快照。 */
    private String actorType;

    /** 事件发生时的登录账号快照。 */
    private String actorAccount;

    /** 事件发生时的显示名称快照。 */
    private String actorName;

    /** 匿名认证请求尝试操作的账号。 */
    private String subjectAccount;

    /** 执行结果：1-成功，2-失败。 */
    private Integer type;

    /** 结构化事件分类：AUTHENTICATION、OPERATION、SECURITY。 */
    private String eventType;

    /** 业务模块。 */
    private String module;

    /** 结构化操作名称。 */
    private String title;

    /** 被操作的业务资源类型或说明。 */
    private String resource;

    /** 客户端 IP。 */
    private String ip;

    /** 客户端 IP 归属地。 */
    private String ipHomePlace;

    /** 客户端 User-Agent。 */
    private String userAgent;

    /** 请求 URI。 */
    private String requestUri;

    /** HTTP 请求方法。 */
    private String method;

    /** 经过脱敏和截断的请求参数。 */
    private String params;

    /** 请求链路标识。 */
    private String traceId;

    /** 预留的调用跨度标识。 */
    private String spanId;

    /** 接口执行耗时，单位毫秒。 */
    private Long time;

    /** 经过脱敏的异常摘要。 */
    private String exception;

    /** 日志创建人；通常与 userId 一致。 */
    @TableField(fill = FieldFill.INSERT)
    private Long createUid;

    /** 日志创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标记。 */
    @TableLogic
    private Boolean isDelete;
}
