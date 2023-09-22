package com.yxx.business.controller;

import com.yxx.business.model.request.AliAppletCloseReq;
import com.yxx.business.model.request.AliPayReq;
import com.yxx.business.model.request.AliRefundOfDuty;
import com.yxx.business.model.response.AliCreatPayRes;
import com.yxx.business.model.response.AliWapPayRes;
import com.yxx.business.model.response.AliWebPayRes;
import com.yxx.business.service.AliPayService;
import com.yxx.common.annotation.auth.ReleaseToken;
import com.yxx.common.annotation.response.ResponseResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付宝支付
 *
 * @author yxx
 * @classname AliPayController
 * @since 2023-09-14 11:41
 */
@Slf4j
@Validated
@ResponseResult
@RestController
@RequestMapping("/aliPay")
@RequiredArgsConstructor
public class AliPayController {

    private final AliPayService aliPayService;

    /**
     * 支付创建，该接口会返回一个支付宝生成的订单号(非本系统生成)
     * 前端拿着该订单号调用my.tradePay方法即可在支付宝小程序中唤起支付弹窗
     *
     * @param req 请求参数
     * @return {@link AliCreatPayRes }
     * @author yxx
     */
    @PostMapping("/pay")
    public AliCreatPayRes pay(@Valid @RequestBody AliPayReq req) {
        return aliPayService.pay(req.getTotalAmount());
    }

    /**
     * 手机网站 支付宝支付
     *
     * @param req 请求参数
     * @return {@link AliWapPayRes }
     * @author yxx
     */
    @PostMapping("/wapPay")
    public AliWapPayRes wapPay(@Valid @RequestBody AliPayReq req) {
        return aliPayService.aliWapPay(req.getTotalAmount());
    }

    /**
     * 电脑网站 支付宝支付
     *
     * @param req 请求参数
     * @return {@link AliWebPayRes }
     * @author yxx
     */
    @PostMapping("/webPay")
    public AliWebPayRes webPay(@Valid @RequestBody AliPayReq req) {
        return aliPayService.webPay(req.getTotalAmount());
    }

    /**
     * 支付宝服务器异步通知url
     *
     * @param request  request
     * @param response response
     * @author yxx
     */
    @ReleaseToken
    @PostMapping(value = "/notifyUrl")
    public void notifyUrl(HttpServletRequest request, HttpServletResponse response) {
        aliPayService.notifyUrl(request, response);
    }

    /**
     * 支付宝退款
     *
     * @param req 请求参数
     * @author yxx
     */
    @PostMapping("/refundOfDuty")
    public void refundOfDuty(@Valid @RequestBody AliRefundOfDuty req) {
        aliPayService.alipayRefundOfDuty(req.getOutTradeNo(), req.getRefundAmount(), req.getRefundReason());
    }

    /**
     * 关闭订单（长时间未支付，调用此方法关闭订单）
     *
     * @param req 商户订单
     * @author yxx
     */
    @PostMapping("/close")
    public void close(@Valid @RequestBody AliAppletCloseReq req) {
        aliPayService.close(req.getOutTradeNo());
    }

}