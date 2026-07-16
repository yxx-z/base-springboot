package com.yxx.framework.audit;

import com.yxx.framework.audit.model.AuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 基于 Spring 应用事件的审计发布器。
 */
@Component
@RequiredArgsConstructor
public class SpringAuditEventPublisher implements AuditEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(AuditEvent event) {
        // 发布器只负责接入 Spring 事件总线，数据库、消息队列或日志由监听器自由扩展。
        applicationEventPublisher.publishEvent(event);
    }
}
