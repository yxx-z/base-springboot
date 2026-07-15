package com.yxx.framework.audit;

import com.yxx.framework.audit.model.AuditEvent;

/**
 * 审计事件发布器。
 */
public interface AuditEventPublisher {

    /**
     * 发布操作审计事件。
     *
     * @param event 审计事件
     */
    void publish(AuditEvent event);
}
