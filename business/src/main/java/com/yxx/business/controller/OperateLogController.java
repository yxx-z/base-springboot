package com.yxx.business.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yxx.business.model.request.OperateLogReq;
import com.yxx.business.model.response.OperateLogResp;
import com.yxx.business.service.OperateLogService;
import com.yxx.common.annotation.response.ResponseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * @author yxx
 * @since 2023-05-17 15:39
 * @description
 *
 * 注：@SaCheckRole("super_admin")
 * 表示只有 super_admin 角色的用户才可访问此controller。
 * 该注解亦可放在具体的方法上
 */
@Slf4j
@Validated
@ResponseResult
@RestController
@RequestMapping("/log")
@RequiredArgsConstructor
@SaCheckRole("super_admin")
public class OperateLogController {
    private final OperateLogService operateLogService;

    @PostMapping("/auth")
    public Page<OperateLogResp> authLogPage(@Valid @RequestBody OperateLogReq req) {
        return operateLogService.authLogPage(req);
    }

    @PostMapping("/operation")
    public Page<OperateLogResp> operationLogPage(@Valid @RequestBody OperateLogReq req) {
        return operateLogService.operationLogPage(req);
    }
}
