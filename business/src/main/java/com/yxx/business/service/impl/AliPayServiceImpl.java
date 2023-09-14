package com.yxx.business.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeCreateRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCreateResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.yxx.business.model.dto.AliPayDto;
import com.yxx.business.service.AliPayService;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.common.properties.AliProperties;
import com.yxx.common.utils.SnowflakeConfig;
import com.yxx.common.utils.auth.AliLoginUtils;
import com.yxx.common.utils.jackson.JacksonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 支付宝支付Service实现类
 *
 * @author yxx
 * @classname AliPayServiceImpl
 * @since 2023-09-14 13:59
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliPayServiceImpl implements AliPayService {

    private final AliProperties aliProperties;

    private final SnowflakeConfig snowflake;

    /**
     * 支付
     *
     * @param totalAmount 支付总金额
     * @return {@link AlipayTradeCreateResponse }
     * @author yxx
     */
    @Override
    public AlipayTradeCreateResponse pay(String totalAmount) {
        //获得初始化的AlipayClient
        AlipayClient alipayClient = new DefaultAlipayClient(aliProperties.getServerUrl(), aliProperties.getAppId(),
                aliProperties.getMerchantPrivateKey(), "json", aliProperties.getCharset(),
                aliProperties.getAlipayPublicKey(), aliProperties.getSignType()
        );
        //设置请求参数
        AlipayTradeCreateRequest request = new AlipayTradeCreateRequest();

        //雪花算法订单号
        long oid = snowflake.snowflakeId();
        AliPayDto payDto = new AliPayDto();
        payDto.setTotalAmount(totalAmount);
        String userId = AliLoginUtils.getLoginCode();
        payDto.setBuyerId(userId);
        payDto.setOutTradeNo(String.valueOf(oid));
        payDto.setSubject("测试");

        //对象转化为json字符串
        String jsonStr = JacksonUtil.toJson(payDto);

        //商户通过该接口进行交易的创建下单
        request.setBizContent(jsonStr);
        //回调地址 是能够访问到的域名加上方法名
        request.setNotifyUrl(aliProperties.getNotifyUrl());
        try {
            //使用的是execute
            AlipayTradeCreateResponse response = alipayClient.execute(request);
            log.info("response:{}", response);
            return response;
        } catch (AlipayApiException e) {
            e.printStackTrace();
            throw new ApiException(ApiCode.SYSTEM_ERROR);
        }
    }

    /**
     * 支付宝服务器异步通知url
     *
     * @param request  request
     * @param response response
     * @author yxx
     */
    @Override
    public void notifyUrl(HttpServletRequest request, HttpServletResponse response) {
        //获取支付宝发送过来的信息
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        Set<Map.Entry<String, String[]>> entries = requestParams.entrySet();
        //循环获取到所有的值
        for (Map.Entry<String, String[]> entry : entries) {
            String name = entry.getKey();
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用
            //valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            params.put(name, valueStr);
        }

        //调用SDK验证签名
        boolean signVerified;
        try {
            signVerified = AlipaySignature.rsaCheckV1(params, aliProperties.getAlipayPublicKey(),
                    aliProperties.getCharset(), aliProperties.getSignType());
        } catch (AlipayApiException e) {
            log.error("调用sdk验证签名异常,异常信息:{}", e.getErrMsg());
            throw new ApiException(ApiCode.SYSTEM_ERROR);
        }
        //boolean类型signVerified为true时 则验证成功
        if (signVerified) {
            //获取到支付的状态 TRADE_SUCCESS则支付成功
            String tradeStatus = request.getParameter("trade_status");
            if (tradeStatus.equals("TRADE_SUCCESS")) {
                log.info("支付成功");
            } else {
                log.error("支付失败");
            }
        }
        //签名验证失败
        else {
            log.error("签名验证失败:{}", AlipaySignature.getSignCheckContentV1(params));
        }
    }

    /**
     * 支付宝退款
     *
     * @param outTradeNo   系统订单号
     * @param refundAmount 退款金额
     * @param refundReason 退款原因
     * @author yxx
     */
    @Override
    public void alipayRefundOfDuty(String outTradeNo, String refundAmount, String refundReason) {
        //初始化
        AlipayClient alipayClient = new DefaultAlipayClient(aliProperties.getServerUrl(), aliProperties.getAppId(),
                aliProperties.getMerchantPrivateKey(), "json", aliProperties.getCharset(),
                aliProperties.getAlipayPublicKey(), aliProperties.getSignType()
        );
        //构造退款的参数
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        //订单号 这里的订单号是自己用算法生成的 和支付宝系统的订单号TradeNo只能二选一
        model.setOutTradeNo(outTradeNo);
        //系统订单号
        //model.setTradeNo(tradeNo);
        //金额
        model.setRefundAmount(refundAmount);
        //退款原因
        model.setRefundReason(refundReason);
        //退款请求
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        //参数set到请求里
        request.setBizModel(model);
        try {
            //退款返回
            AlipayTradeRefundResponse response = alipayClient.execute(request);
            log.info("订单信息:{}", response.getBody());//获取到body这个订单的所有信息
            log.info("退款信息:{}", response.getMsg()); //这里打印的是退款的信息 是否退款成功的原因
        } catch (AlipayApiException e) {
            log.error("支付宝退款失败，异常信息:{}", e.getErrMsg());
            throw new ApiException(ApiCode.SYSTEM_ERROR);
        }
    }
}
