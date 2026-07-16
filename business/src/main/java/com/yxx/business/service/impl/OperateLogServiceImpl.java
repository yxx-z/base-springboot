package com.yxx.business.service.impl;

import com.yxx.business.mapper.OperateLogMapper;
import com.yxx.business.model.entity.OperateLog;
import com.yxx.framework.audit.model.AuditEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

/**
 * 操作日志表 服务实现类
 *
 * @author yxx
 * @since 2022-07-15
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OperateLogServiceImpl {

    private final OperateLogMapper operateLogMapper;

    /**
     * 异步持久化用户端操作审计事件。
     *
     * @param event 审计事件
     */
    @Async("auditTaskExecutor")
    @EventListener
    public void saveAuditEvent(AuditEvent event) {
        // 在异步线程中把通用事件复制为用户端日志实体，避免审计模块依赖业务表结构。
        OperateLog operateLog = new OperateLog();
        operateLog.setUserId(event.actor() == null ? null : event.actor().actorId());
        operateLog.setCreateUid(event.actor() == null ? null : event.actor().actorId());
        operateLog.setActorType(event.actor() == null ? null : event.actor().actorType());
        operateLog.setActorAccount(event.actor() == null ? null : event.actor().actorAccount());
        operateLog.setActorName(event.actor() == null ? null : event.actor().actorName());
        operateLog.setSubjectAccount(event.subjectAccount());
        operateLog.setSubjectType(event.subjectType());
        operateLog.setSubjectId(event.subjectId());
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
        try {
            // save 返回 false 与抛出异常分别记录，二者均不能传播到已结束的业务请求。
            if (operateLogMapper.insert(operateLog) != 1) {
                log.error("用户端审计事件未写入数据库，traceId={}", event.traceId());
            }
        } catch (RuntimeException exception) {
            // 审计持久化为异步辅助链路，失败必须记录，但不能反向影响已经完成的业务事务。
            log.error("用户端审计事件持久化失败，traceId={}", event.traceId(), exception);
        }
    }
}
