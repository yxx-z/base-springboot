package com.yxx.business.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.business.mapper.OperateLogMapper;
import com.yxx.business.model.request.OperateLogReq;
import com.yxx.business.model.response.OperateLogResp;
import com.yxx.business.service.OperateLogService;
import com.yxx.common.core.model.LogDTO;
import com.yxx.common.core.model.OperateLog;
import com.yxx.framework.service.OperationLogService;
import org.springframework.beans.BeanUtils;
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
        implements OperateLogService, OperationLogService {

    @Async
    @Override
    public void saveLog(LogDTO dto) {
        OperateLog log = new OperateLog();
        BeanUtils.copyProperties(dto, log);
        this.save(log);
    }


    @Override
    public Page<OperateLogResp> getOperateLog(Page<OperateLogResp> page, OperateLogReq req) {
        return baseMapper.getOperateLog(page, req);
    }
}
