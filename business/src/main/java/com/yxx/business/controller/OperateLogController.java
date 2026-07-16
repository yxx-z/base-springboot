package com.yxx.business.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.yxx.business.model.request.OperateLogReq;
import com.yxx.business.model.response.OperateLogResp;
import com.yxx.business.service.OperateLogService;
import com.yxx.business.security.UserSecurityCodes;
import com.yxx.common.annotation.response.ResponseResult;
import com.yxx.common.core.page.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * @author yxx
 * 用户端审计日志查询接口，访问者必须具备审计日志读取权限。
 * @since 2023-05-17 15:39
 */
@Validated
@ResponseResult
@RestController
@RequestMapping("/log")
@RequiredArgsConstructor
@SaCheckPermission(UserSecurityCodes.PERMISSION_AUDIT_LOG_READ)
public class OperateLogController {
    private final OperateLogService operateLogService;

    /**
     * 身份验证登录日志数据分页
     *
     * @param req 要求事情
     * @return {@link PageResponse }<{@link OperateLogResp }>
     * @author yxx
     */
    @PostMapping("/auth")
    public PageResponse<OperateLogResp> authLogPage(@Valid @RequestBody OperateLogReq req) {
        return operateLogService.authLogPage(req);
    }

    /**
     * 操作日志数据分页
     *
     * @param req 要求事情
     * @return {@link PageResponse }<{@link OperateLogResp }>
     * @author yxx
     */
    @PostMapping("/operation")
    public PageResponse<OperateLogResp> operationLogPage(@Valid @RequestBody OperateLogReq req) {
        return operateLogService.operationLogPage(req);
    }
}
