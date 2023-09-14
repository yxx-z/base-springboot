package com.yxx.business.service;

import com.alipay.api.response.AlipayTradeCreateResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author yxx
 * @since 2023-09-14 13:58
 */
public interface AliPayService {

    /**
     * 支付宝支付
     *
     * @param totalAmount 支付总额
     * @return {@link AlipayTradeCreateResponse }
     * @author yxx
     */
    AlipayTradeCreateResponse pay(String totalAmount);

    /**
     * 支付宝支付异步回调
     *
     * @param request  请求流
     * @param response 返回流
     * @author yxx
     */
    void notifyUrl(HttpServletRequest request, HttpServletResponse response);

    /**
     * 支付宝退款
     *
     * @param outTradeNo   本系统自己生成的订单号
     * @param refundAmount 退款金额
     * @param refundReason 退款原因
     * @author yxx
     */
    void alipayRefundOfDuty(String outTradeNo, String refundAmount, String refundReason);

}
