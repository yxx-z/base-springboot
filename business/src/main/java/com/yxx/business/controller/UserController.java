package com.yxx.business.controller;

import com.yxx.business.model.request.*;
import com.yxx.business.service.UserService;
import com.yxx.common.annotation.auth.ReleaseToken;
import com.yxx.common.annotation.log.OperationLog;
import com.yxx.common.annotation.response.ResponseResult;
import com.yxx.common.core.model.LoginUser;
import com.yxx.common.utils.auth.LoginUtils;
import jakarta.validation.Valid;
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

    /**
     * 注册
     *
     * @param req 要求事情
     * @return {@link Boolean }
     * @author yxx
     */
    @ReleaseToken
    @OperationLog(module = "用户模块", title = "用户注册")
    @PostMapping("/register")
    public Boolean register(@Valid @RequestBody UserRegisterReq req) {
        return userService.register(req);
    }

    /**
     * 发送注册验证码
     *
     * @param req 要求事情
     * @return {@link Boolean }
     * @author yxx
     */
    @ReleaseToken
    @OperationLog(module = "用户模块", title = "发送注册邮箱验证码")
    @PostMapping("/sendCaptcha")
    public Boolean sendRegisterCaptcha(@Valid @RequestBody RegisterCaptchaReq req){
        return userService.sendRegisterCaptcha(req);
    }

    /**
     * 获取用户信息
     *
     * @return {@link LoginUser }
     * @author yxx
     */
    @OperationLog(module = "用户模块", title = "获取用户信息")
    @GetMapping("/info")
    public LoginUser info() {
        Long userId = LoginUtils.getUserId();
        log.info("userId为[{}]", userId);
        return LoginUtils.getLoginUser();
    }

    /**
     * 发送重置密码邮件
     *
     * @param req 要求事情
     * @return {@link Boolean }
     * @author yxx
     */
    @ReleaseToken
    @OperationLog(module = "用户模块", title = "发送重置密码邮件")
    @PostMapping("/resetPwdEmail")
    public Boolean resetPwdEmail(@Valid @RequestBody ResetPwdEmailReq req){
        return userService.resetPwdEmail(req);
    }

    /**
     * 重置密码
     *
     * @param req 要求事情
     * @return {@link Boolean }
     * @author yxx
     */
    @ReleaseToken
    @OperationLog(module = "用户模块", title = "重置密码")
    @PostMapping("/resetPwd")
    public Boolean resetPwd(@Valid @RequestBody ResetPwdReq req){
        return userService.resetPwd(req);
    }

    /**
     * 修改密码
     *
     * @param req 要求事情
     * @return {@link Boolean }
     * @author yxx
     */
    @OperationLog(module = "用户模块", title = "修改密码")
    @PostMapping("/editPwd")
    public Boolean editPwd(@Valid @RequestBody EditPwdReq req){
        return userService.editPwd(req);
    }

}
