package com.yxx.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yxx.admin.mapper.OperateAdminLogMapper;
import com.yxx.admin.model.request.OperateLogReq;
import com.yxx.admin.model.response.OperateLogResp;
import com.yxx.admin.service.OperateAdminLogService;
import com.yxx.common.core.page.PageResponse;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * 操作日志表 服务实现类
 *
 * @author yxx
 * @since 2022-07-15
 */
@Service
@RequiredArgsConstructor
public class OperateAdminLogServiceImpl implements OperateAdminLogService {

    private final OperateAdminLogMapper operateAdminLogMapper;

    @Override
    public PageResponse<OperateLogResp> operationLogPage(OperateLogReq req) {
        // Page 作为 Mapper 分页入参，并由插件回填总数与总页数。
        Page<OperateLogResp> page = new Page<>(req.getPage(), req.getPageSize());
        // 转换为框架公共分页模型，隔离持久化层类型。
        Page<OperateLogResp> result = operateAdminLogMapper.operationLogPage(page, req);
        return new PageResponse<>(result.getRecords(), result.getCurrent(), result.getSize(),
                result.getTotal(), result.getPages());
    }

    @Override
    public PageResponse<OperateLogResp> authLogPage(OperateLogReq req) {
        // 登录类事件使用独立查询，但复用统一分页返回结构。
        Page<OperateLogResp> page = new Page<>(req.getPage(), req.getPageSize());
        Page<OperateLogResp> result = operateAdminLogMapper.authLogPage(page, req);
        return new PageResponse<>(result.getRecords(), result.getCurrent(), result.getSize(),
                result.getTotal(), result.getPages());
    }
}
