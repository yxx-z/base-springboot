package com.yxx.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.admin.mapper.OperateAdminLogMapper;
import com.yxx.admin.model.request.OperateLogReq;
import com.yxx.admin.model.response.OperateLogResp;
import com.yxx.admin.service.OperateAdminLogService;
import com.yxx.admin.model.entity.OperateAdminLog;
import com.yxx.framework.audit.model.AuditEvent;
import com.yxx.common.core.page.PageResponse;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * 操作日志表 服务实现类
 *
 * @author yxx
 * @since 2022-07-15
 */
@Service
@Slf4j
public class OperateAdminLogServiceImpl extends ServiceImpl<OperateAdminLogMapper, OperateAdminLog>
        implements OperateAdminLogService {

    /**
     * 异步持久化管理端操作审计事件。
     *
     * @param event 审计事件
     */
    @Async("auditTaskExecutor")
    @EventListener
    public void saveAuditEvent(AuditEvent event) {
        OperateAdminLog operateLog = new OperateAdminLog();
        operateLog.setUserId(event.actor() == null ? null : event.actor().actorId());
        operateLog.setCreateUid(event.actor() == null ? null : event.actor().actorId());
        operateLog.setActorType(event.actor() == null ? null : event.actor().actorType());
        operateLog.setActorAccount(event.actor() == null ? null : event.actor().actorAccount());
        operateLog.setActorName(event.actor() == null ? null : event.actor().actorName());
        operateLog.setSubjectAccount(event.subjectAccount());
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
            if (!save(operateLog)) {
                log.error("管理端审计事件未写入数据库，traceId={}", event.traceId());
            }
        } catch (RuntimeException exception) {
            // 审计持久化为异步辅助链路，失败必须记录，但不能反向影响已经完成的业务事务。
            log.error("管理端审计事件持久化失败，traceId={}", event.traceId(), exception);
        }
    }


    @Override
    public PageResponse<OperateLogResp> operationLogPage(OperateLogReq req) {
        // 初始化分页构造器
        Page<OperateLogResp> page = new Page<>(req.getPage(), req.getPageSize());
        // 查询分页结果并返回
        Page<OperateLogResp> result = baseMapper.operationLogPage(page, req);
        return new PageResponse<>(result.getRecords(), result.getCurrent(), result.getSize(),
                result.getTotal(), result.getPages());
    }

    @Override
    public PageResponse<OperateLogResp> authLogPage(OperateLogReq req) {
        // 初始化分页构造器
        Page<OperateLogResp> page = new Page<>(req.getPage(), req.getPageSize());
        // 查询分页结果并返回
        Page<OperateLogResp> result = this.baseMapper.authLogPage(page, req);
        return new PageResponse<>(result.getRecords(), result.getCurrent(), result.getSize(),
                result.getTotal(), result.getPages());
    }
}
