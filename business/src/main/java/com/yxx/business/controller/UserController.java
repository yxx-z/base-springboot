package com.yxx.business.controller;

import com.yxx.business.model.request.UserRegisterReq;
import com.yxx.business.service.UserService;
import com.yxx.common.annotation.auth.ReleaseToken;
import com.yxx.common.annotation.log.OperationLog;
import com.yxx.common.annotation.response.ResponseResult;
import com.yxx.common.core.model.LoginUser;
import com.yxx.common.utils.auth.LoginUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author yxx
 * @since 2022-11-12 02:07
 */
@Slf4j
@Validated
@ResponseResult
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @ReleaseToken
    @OperationLog(module = "用户模块", title = "用户注册")
    @PostMapping("/register")
    public Boolean register(@RequestBody UserRegisterReq req) {
        return userService.register(req);
    }


    @OperationLog(module = "用户模块", title = "获取用户信息")
    @GetMapping("/info")
    public LoginUser info() {
        Long userId = LoginUtils.getUserId();
        log.info("userId为[{}]", userId);
        return LoginUtils.getLoginUser();
    }

}
