package com.yxx.business.controller;

import com.yxx.business.model.request.AliPayReq;
import com.yxx.business.model.request.AliRefundOfDuty;
import com.yxx.business.model.response.AliCreatPayRes;
import com.yxx.business.service.AliAppletPayService;
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
public class AliAppletPayController {

    private final AliAppletPayService aliAppletPayService;

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
        return aliAppletPayService.pay(req.getTotalAmount());
    }

    /**
     * 支付宝服务器异步通知url
     *
     * @param request  request
     * @param response response
     * @author yxx
     */
    @PostMapping(value = "/notifyUrl")
    public void notifyUrl(HttpServletRequest request, HttpServletResponse response) {
        aliAppletPayService.notifyUrl(request, response);
    }

    /**
     * 支付宝退款
     *
     * @param req 请求参数
     * @author yxx
     */
    @PostMapping("/refundOfDuty")
    public void refundOfDuty(@Valid @RequestBody AliRefundOfDuty req) {
        aliAppletPayService.alipayRefundOfDuty(req.getOutTradeNo(), req.getRefundAmount(), req.getRefundReason());
    }

}
