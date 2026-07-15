package com.yxx.framework.service;


import com.yxx.common.core.model.LogDTO;

/**
 *
 * @author yxx
 * @since 2022/7/26 11:29
 */
public interface OperationLogService {

    /**
     * 保存操作日志
     *
     * @param dto LogDTO
     */
    void saveLog(LogDTO dto);
}
