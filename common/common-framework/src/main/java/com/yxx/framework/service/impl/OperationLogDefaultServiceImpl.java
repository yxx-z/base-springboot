package com.yxx.framework.service.impl;

import com.yxx.common.core.model.LogDTO;
import com.yxx.common.utils.jackson.JacksonUtil;
import com.yxx.framework.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author yxx
 * @since 2022/7/26 14:44
 */
@Slf4j
public class OperationLogDefaultServiceImpl implements OperationLogService {

    @Override
    public void saveLog(LogDTO dto) {
        log.info("操作日志====> {}", JacksonUtil.toJson(dto));
    }
}
