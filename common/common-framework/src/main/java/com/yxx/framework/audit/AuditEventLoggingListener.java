package com.yxx.framework.audit;

import com.yxx.framework.audit.model.AuditEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 审计事件结构化日志监听器。
 *
 * <p>数据库监听器由具体应用提供；该监听器作为统一日志备份，便于数据库暂时不可用时排查。</p>
 */
@Slf4j
@Component
public class AuditEventLoggingListener {

    @EventListener
    public void logEvent(AuditEvent event) {
        log.info("操作审计 module={} action={} actorId={} actorType={} resultType={} durationMs={} traceId={}",
                event.module(), event.action(),
                event.actor() == null ? null : event.actor().actorId(),
                event.actor() == null ? null : event.actor().actorType(),
                event.type(), event.durationMillis(), event.traceId());
    }
}
