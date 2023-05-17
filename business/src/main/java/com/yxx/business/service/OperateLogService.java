package com.yxx.business.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yxx.business.model.request.OperateLogReq;
import com.yxx.business.model.response.OperateLogResp;
import com.yxx.common.core.model.OperateLog;

/**
 * 操作日志表 服务类
 *
 * @author yxx
 * @since 2022-07-15
 */
public interface OperateLogService extends IService<OperateLog> {

    /**
     * 查询操作日志分页列表
     *
     * @param req  OperateLogReq
     * @return 操作日志分页列表
     */
    Page<OperateLogResp> operationLogPage(OperateLogReq req);

    /**
     * 登录日志分野
     *
     * @param req 请求参数
     * @return 分页结果
     */
    Page<OperateLogResp> authLogPage(OperateLogReq req);
}
