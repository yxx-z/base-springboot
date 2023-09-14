package com.yxx.business.service;

import com.yxx.business.model.response.AliCreatPayRes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author yxx
 * @since 2023-09-14 13:58
 */
public interface AliAppletPayService {

    /**
     * 支付创建，该接口会返回一个支付宝生成的订单号(非本系统生成)
     * 前端拿着该订单号调用my.tradePay方法即可在支付宝小程序中唤起支付弹窗
     *
     * @param totalAmount 支付总金额
     * @return {@link AliCreatPayRes }
     * @author yxx
     */
    AliCreatPayRes pay(String totalAmount);

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
