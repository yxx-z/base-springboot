package com.yxx.business.model.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 支付宝退款请求参数
 *
 * @author yxx
 * @classname AliRefundOfDuty
 * @since 2023-09-14 13:53
 */
@Data
public class AliRefundOfDuty {
    /**
     * 自己系统生成的订单号
     */
    @NotBlank(message = "系统订单号不能为空")
    private String outTradeNo;

    /**
     * 退款金额
     */
    @NotBlank(message = "退款金额不能为空")
    private String refundAmount;

    /**
     * 退款原因
     */
    @NotBlank(message = "退款原因不能为空")
    private String refundReason;
}
