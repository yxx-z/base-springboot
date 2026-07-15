package com.yxx.business.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.business.mapper.OperateLogMapper;
import com.yxx.business.model.request.OperateLogReq;
import com.yxx.business.model.response.OperateLogResp;
import com.yxx.business.service.OperateLogService;
import com.yxx.common.core.model.OperateLog;
import com.yxx.framework.audit.model.AuditEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 操作日志表 服务实现类
 *
 * @author yxx
 * @since 2022-07-15
 */
@Service
public class OperateLogServiceImpl extends ServiceImpl<OperateLogMapper, OperateLog>
        implements OperateLogService {

    /**
     * 异步持久化用户端操作审计事件。
     *
     * @param event 审计事件
     */
    @Async("applicationTaskExecutor")
    @EventListener
    public void saveAuditEvent(AuditEvent event) {
        OperateLog operateLog = new OperateLog();
        operateLog.setUserId(event.actor() == null ? null : event.actor().actorId());
        operateLog.setCreateUid(event.actor() == null ? null : event.actor().actorId());
        operateLog.setType(event.type());
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
        save(operateLog);
    }


    @Override
    public Page<OperateLogResp> operationLogPage(OperateLogReq req) {
        // 初始化分页构造器
        Page<OperateLogResp> page = new Page<>(req.getPage(), req.getPageSize());
        // 查询分页结果并返回
        return baseMapper.operationLogPage(page, req);
    }

    @Override
    public Page<OperateLogResp> authLogPage(OperateLogReq req) {
        // 初始化分页构造器
        Page<OperateLogResp> page = new Page<>(req.getPage(), req.getPageSize());
        // 查询分页结果并返回
        return this.baseMapper.authLogPage(page, req);
    }
}
