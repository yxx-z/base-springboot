package com.yxx.admin.audit;

import com.yxx.admin.mapper.OperateAdminLogMapper;
import com.yxx.admin.model.entity.OperateAdminLog;
import com.yxx.framework.audit.model.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 管理端审计事件持久化监听器。
 *
 * <p>该组件只负责把公共审计模块发布的 {@link AuditEvent} 转换为管理端日志实体并写入数据库。
 * 将事件消费职责与日志查询服务分离，可以避免查询接口被异步监听方法污染，同时保证使用
 * JDK 动态代理时，Spring 仍能够在实际 Bean 类型上找到事件监听方法。</p>
 *
 * <p>操作日志属于普通的辅助审计能力，采用 best-effort 语义：写入失败会完整记录错误，
 * 但不会回滚或改变已经完成的业务操作。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuditEventPersistenceListener {

    private final OperateAdminLogMapper operateAdminLogMapper;

    /**
     * 异步持久化管理端操作审计事件。
     *
     * @param event 公共审计模块发布的不可变审计事件
     */
    @Async("auditTaskExecutor")
    @EventListener
    public void saveAuditEvent(AuditEvent event) {
        // 在应用边界完成模型转换，使公共审计模块无需依赖管理端数据库表结构。
        OperateAdminLog operateLog = convertToEntity(event);
        try {
            // Mapper 返回值不是 1 时同样视为持久化失败，避免静默丢失操作日志。
            if (operateAdminLogMapper.insert(operateLog) != 1) {
                log.error("管理端审计事件未写入数据库，traceId={}", event.traceId());
            }
        } catch (RuntimeException exception) {
            // 日志为辅助链路，异常只在服务端记录，不能反向影响已经完成的业务事务。
            log.error("管理端审计事件持久化失败，traceId={}", event.traceId(), exception);
        }
    }

    /**
     * 将通用审计事件转换为管理端日志实体。
     *
     * @param event 通用审计事件
     * @return 可直接持久化的管理端日志实体
     */
    private OperateAdminLog convertToEntity(AuditEvent event) {
        OperateAdminLog operateLog = new OperateAdminLog();

        // 未登录请求可能没有操作者信息，因此操作者相关字段必须允许为空。
        if (event.actor() != null) {
            operateLog.setUserId(event.actor().actorId());
            operateLog.setCreateUid(event.actor().actorId());
            operateLog.setActorType(event.actor().actorType());
            operateLog.setActorAccount(event.actor().actorAccount());
            operateLog.setActorName(event.actor().actorName());
        }

        // 被操作主体与操作者相互独立，例如管理员停用某个业务用户时需要同时保留两者。
        operateLog.setSubjectAccount(event.subjectAccount());
        operateLog.setSubjectType(event.subjectType());
        operateLog.setSubjectId(event.subjectId());

        // 记录业务语义、请求上下文与执行结果，供后续查询和问题排查使用。
        operateLog.setType(event.type());
        operateLog.setEventType(event.eventType().name());
        operateLog.setModule(event.module());
        operateLog.setTitle(event.action());
        operateLog.setResource(event.resource());
        operateLog.setIp(event.ip());
        operateLog.setIpHomePlace(event.ipRegion());
        operateLog.setUserAgent(event.userAgent());
        operateLog.setRequestUri(event.requestUri());
        operateLog.setMethod(event.httpMethod());
        operateLog.setParams(event.requestParams());
        operateLog.setTraceId(event.traceId());
        operateLog.setTime(event.durationMillis());
        operateLog.setException(event.exceptionMessage());
        return operateLog;
    }
}
