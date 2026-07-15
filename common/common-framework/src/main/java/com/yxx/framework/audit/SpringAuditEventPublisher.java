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
        applicationEventPublisher.publishEvent(event);
    }
}
