package com.yxx.business.controller;

import com.yxx.business.model.request.AliGetUserIdReq;
import com.yxx.business.model.request.AliQueryUserReq;
import com.yxx.business.model.response.AliUserIdRes;
import com.yxx.business.model.response.LoginRes;
import com.yxx.business.service.AliUserService;
import com.yxx.common.annotation.response.ResponseResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付宝小程序鉴权
 *
 * @author yxx
 * @classname AliAuthController
 * @since 2023-09-14 11:13
 */
@Slf4j
@Validated
@ResponseResult
@RestController
@RequestMapping("/aliAuth")
@RequiredArgsConstructor
public class AliAuthController {

    private final AliUserService aliUserService;


    /**
     * 授权码换取userId（授权码authCode由前端获取）
     *
     * @param req 请求参数
     * @return {@link AliUserIdRes }
     * @author yxx
     */
    @PostMapping("/getInfo")
    public AliUserIdRes getInfo(@Valid @RequestBody AliGetUserIdReq req) {
        return aliUserService.getUserIdByAuthCode(req.getAuthCode());
    }

    /**
     * *
     * 根据前端获取的支付宝授权信息去查询数据库是否有这个人，有就更新时间 没有就添加到数据库
     *
     * @param req 请求参数
     * @return {@link LoginRes }
     * @author yxx
     */
    @PostMapping("/login")
    public LoginRes login(@Valid @RequestBody AliQueryUserReq req) {
        return aliUserService.aliLogin(req);
    }
}
