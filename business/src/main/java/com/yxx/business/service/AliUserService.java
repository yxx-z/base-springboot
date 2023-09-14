package com.yxx.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxx.business.model.entity.AliUser;
import com.yxx.business.model.request.AliQueryUserReq;
import com.yxx.business.model.response.AliUserIdRes;
import com.yxx.business.model.response.LoginRes;

/**
 * @author yxx
 * @since 2023-09-14 10:30
 */
public interface AliUserService extends IService<AliUser> {
    /**
     * 根据支付宝小程序授权码 获取用户唯一标识 userId
     *
     * @param authCode 授权码
     * @return {@link AliUserIdRes }
     * @author yxx
     */
    AliUserIdRes getUserIdByAuthCode(String authCode);

    /**
     * 根据支付宝小程序中用户唯一标识 判断用户是否存在，存在则走登录流程。不存在保存数据库后走登录流程
     *
     * @param req 请求参数
     * @return {@link LoginRes }
     * @author yxx
     */
    LoginRes aliLogin(AliQueryUserReq req);

}
