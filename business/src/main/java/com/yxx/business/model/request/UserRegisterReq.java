package com.yxx.business.model.request;

import lombok.Data;

/**
 * @author yxx
 * @since 2023-05-15 14:11
 */
@Data
public class UserRegisterReq {
    /**
     * 登录账号
     */
    private String loginCode;

    /**
     * 登录名
     */
    private String loginName;

    /**
     * 密码
     */
    private String password;

    /**
     * 手机号
     */
    private String linkPhone;
}
