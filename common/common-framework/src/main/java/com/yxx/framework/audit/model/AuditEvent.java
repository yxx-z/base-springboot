package com.yxx.framework.audit.model;

import com.yxx.security.model.CurrentActor;

/**
 * 不可变操作审计事件。
 *
 * <p>事件是审计切面与数据库、消息队列或结构化日志处理器之间的稳定契约。</p>
 */
public record AuditEvent(
        CurrentActor actor,
        String module,
        String action,
        String resource,
        Integer type,
        String ip,
        String ipRegion,
        String userAgent,
        String requestUri,
        String httpMethod,
        String requestParams,
        String traceId,
        Long durationMillis,
        String exceptionMessage) {
}
