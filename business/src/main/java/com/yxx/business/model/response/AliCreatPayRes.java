package com.yxx.business.model.response;

import lombok.Data;

/**
 * 支付宝创建订单返回数据
 *
 * @author yxx
 * @classname AliCreatPayRes
 * @since 2023-09-14 17:27
 */
@Data
public class AliCreatPayRes {
    /**
     * 本系统生成的订单号
     */
    private String outTradeNo;

    /**
     * 支付宝生成的订单号
     */
    private String tradeNo;
}
