package com.yxx.business.controller;

import com.yxx.business.model.request.LoginReq;
import com.yxx.business.model.response.LoginRes;
import com.yxx.business.service.UserService;
import com.yxx.common.annotation.auth.ReleaseToken;
import com.yxx.common.annotation.log.OperationLog;
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
 * @since 2023-05-17 10:02
 */
@Slf4j
@Validated
@ResponseResult
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    /**
     * 登录
     *
     * @param request 请求
     * @return {@link LoginRes }
     * @author yxx
     */
    @ReleaseToken
    @OperationLog(module = "鉴权模块", title = "pc登录")
    @PostMapping("/login")
    public LoginRes login(@Valid @RequestBody LoginReq request) {
        return userService.login(request);
    }

}
