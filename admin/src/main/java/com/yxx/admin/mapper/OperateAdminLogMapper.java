package com.yxx.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yxx.admin.model.request.OperateLogReq;
import com.yxx.admin.model.response.OperateLogResp;
import com.yxx.common.core.model.OperateAdminLog;
import org.apache.ibatis.annotations.Param;

/**
 * 操作日志表 Mapper 接口
 *
 * @author yxx
 * @since 2022-07-15
 */
public interface OperateAdminLogMapper extends BaseMapper<OperateAdminLog> {

    /**
     * 查询操作日志分页列表
     *
     * @param page 分页参数
     * @param req  OperateLogReq
     * @return 操作日志分页列表
     */
    Page<OperateLogResp> operationLogPage(@Param("page") Page<OperateLogResp> page,
                                          @Param("req") OperateLogReq req);

    /**
     * 登录日志分页
     *
     * @param page 分页构造器
     * @param req  请求参数
     * @return 分页结果
     */
    Page<OperateLogResp> authLogPage(@Param("page") Page<OperateLogResp> page, @Param("req") OperateLogReq req);
}
