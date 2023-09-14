package com.yxx.business.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 支付宝支付参数
 *
 * @author yxx
 * @classname AliPayDto
 * @since 2023-09-14 11:50
 */
@Data
public class AliPayDto {
    /**
     * 订单号
     */
    @JsonProperty(value = "out_trade_no")
    private String outTradeNo;

    /**
     * 交易总金额
     */
    @JsonProperty(value = "total_amount")
    private String totalAmount;

    /**
     * 标题
     */
    private String subject;

    /**
     * 用户唯一标识
     */
    @JsonProperty(value = "buyer_id")
    private String buyerId;

}