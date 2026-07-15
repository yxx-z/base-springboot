package com.yxx.business.service;

import com.yxx.business.model.response.AliCreatPayRes;
import com.yxx.business.model.response.AliWapPayRes;
import com.yxx.business.model.response.AliWebPayRes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.math.BigDecimal;

/**
 * @author yxx
 * @since 2023-09-14 13:58
 */
public interface AliPayService {

    /**
     * 支付创建，该接口会返回一个支付宝生成的订单号(非本系统生成)
     * 前端拿着该订单号调用my.tradePay方法即可在支付宝小程序中唤起支付弹窗
     *
     * @param totalAmount 支付总金额
     * @return {@link AliCreatPayRes }
     * @author yxx
     */
    AliCreatPayRes pay(BigDecimal totalAmount);

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

    /**
     * 关闭订单（长时间未支付，调用此方法关闭订单）
     *
     * @param outTradeNo 商户订单
     * @author yxx
     */
    void close(String outTradeNo);

    /**
     * 手机网站调用支付支付
     *
     * @param totalAmount 总金额
     * @return {@link AliWapPayRes }
     * @author yxx
     */
    AliWapPayRes aliWapPay(BigDecimal totalAmount);

    /**
     * 电脑网站 支付宝支付
     *
     * @param totalAmount 支付总金额
     * @return {@link AliWebPayRes }
     * @author yxx
     */
    AliWebPayRes webPay(BigDecimal totalAmount);
}
