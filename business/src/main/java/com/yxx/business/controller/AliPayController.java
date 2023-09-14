package com.yxx.business.controller;

import com.yxx.business.model.request.AliPayReq;
import com.yxx.business.model.request.AliRefundOfDuty;
import com.yxx.business.service.AliPayService;
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
     * 支付
     *
     * @param req 请求参数
     * @return {@link String }
     * @author yxx
     */
    @PostMapping("/pay")
    public String pay(@Valid @RequestBody AliPayReq req) {
        return aliPayService.pay(req.getTotalAmount());
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

}
