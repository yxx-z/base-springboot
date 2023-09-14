package com.yxx.business.model.request;

import lombok.Data;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

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
    @NotBlank(message = "支付金额不能为空")
    private String totalAmount;
}
