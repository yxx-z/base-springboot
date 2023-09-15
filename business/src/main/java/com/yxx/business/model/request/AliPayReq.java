package com.yxx.business.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付宝支付参数
 *
 * @author yxx
 * @classname AliPayReq
 * @since 2023-09-14 13:36
 */
@Data
public class AliPayReq {

    /**
     * 支付总金额
     */
    @NotNull(message = "支付金额不能为空")
    private BigDecimal totalAmount;
}
