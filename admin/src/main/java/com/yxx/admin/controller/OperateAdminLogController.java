package com.yxx.admin.controller;

import com.yxx.admin.model.request.OperateLogReq;
import com.yxx.admin.model.response.OperateLogResp;
import com.yxx.admin.service.OperateAdminLogService;
import com.yxx.common.annotation.response.ResponseResult;
import com.yxx.common.core.page.PageResponse;
import com.yxx.security.annotation.SaAdminCheckPermission;
import com.yxx.admin.security.AdminSecurityCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * @author yxx
 * 管理端审计日志查询接口，访问者必须具备审计日志读取权限。
 * @since 2023-05-17 15:39
 */
@Validated
@ResponseResult
@RestController
@RequestMapping("/log")
@RequiredArgsConstructor
@SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_AUDIT_LOG_READ)
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
    public PageResponse<OperateLogResp> authLogPage(@Valid @RequestBody OperateLogReq req) {
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
    public PageResponse<OperateLogResp> operationLogPage(@Valid @RequestBody OperateLogReq req) {
        return operateAdminLogService.operationLogPage(req);
    }
}
