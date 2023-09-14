package com.yxx.business.model.request;

import lombok.Data;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

/**
 * 支付宝小程序关闭订单请求参数
 *
 * @author yxx
 * @classname AliAppletCloseReq
 * @since 2023-09-14 20:07
 */
@Data
public class AliAppletCloseReq {

    /**
     * 商户订单号
     */
    @NotBlank(message = "商户订单号不能为空")
    private String outTradeNo;
}
