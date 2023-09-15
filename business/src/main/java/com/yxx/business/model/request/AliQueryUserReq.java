package com.yxx.business.model.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 前端获取的支付宝授权信息
 *
 * @author yxx
 * @classname AliQueryUserReq
 * @since 2023-09-14 10:48
 */
@Data
public class AliQueryUserReq {
    /**
     * 支付宝小程序用户唯一标识
     */
    @NotBlank(message = "支付宝小程序用户唯一标识不能为空")
    private String userId;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 国家
     */
    private String countryCode;

    /**
     * 范围
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    private String nickName;

    /**
     * 性别
     */
    private String gender;
}
