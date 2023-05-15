package com.yxx.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yxx.business.model.request.OperateLogReq;
import com.yxx.business.model.response.OperateLogResp;
import com.yxx.common.core.model.OperateLog;
import org.apache.ibatis.annotations.Param;

/**
 * 操作日志表 Mapper 接口
 *
 * @author yxx
 * @since 2022-07-15
 */
public interface OperateLogMapper extends BaseMapper<OperateLog> {

    /**
     * 查询操作日志分页列表
     *
     * @param page 分页参数
     * @param req  OperateLogReq
     * @return 操作日志分页列表
     */
    Page<OperateLogResp> getOperateLog(Page<OperateLogResp> page,
                                       @Param("req") OperateLogReq req);
}
