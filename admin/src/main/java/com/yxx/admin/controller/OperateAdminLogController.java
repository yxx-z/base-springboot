package com.yxx.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yxx.admin.model.request.OperateLogReq;
import com.yxx.admin.model.response.OperateLogResp;
import com.yxx.admin.service.OperateAdminLogService;
import com.yxx.common.annotation.response.ResponseResult;
import com.yxx.common.annotation.satoken.SaAdminCheckRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * @author yxx
 * @description 注：@SaCheckRole("super_admin")
 * 表示只有 super_admin 角色的用户才可访问此controller。
 * 该注解亦可放在具体的方法上
 * @since 2023-05-17 15:39
 */
@Slf4j
@Validated
@ResponseResult
@RestController
@RequestMapping("/log")
@RequiredArgsConstructor
@SaAdminCheckRole("super_admin")
public class OperateAdminLogController {
    private final OperateAdminLogService operateAdminLogService;

    /**
     * 身份验证登录日志数据分页
     *
     * @param req 要求事情
     * @return {@link Page }<{@link OperateLogResp }>
     * @author yxx
     */
    @PostMapping("/auth")
    public Page<OperateLogResp> authLogPage(@Valid @RequestBody OperateLogReq req) {
        return operateAdminLogService.authLogPage(req);
    }

    /**
     * 操作日志数据分页
     *
     * @param req 要求事情
     * @return {@link Page }<{@link OperateLogResp }>
     * @author yxx
     */
    @PostMapping("/operation")
    public Page<OperateLogResp> operationLogPage(@Valid @RequestBody OperateLogReq req) {
        return operateAdminLogService.operationLogPage(req);
    }
}
