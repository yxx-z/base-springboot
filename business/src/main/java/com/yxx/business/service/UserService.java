package com.yxx.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxx.business.model.entity.User;
import com.yxx.business.model.request.LoginReq;
import com.yxx.business.model.request.UserRegisterReq;
import com.yxx.business.model.response.LoginRes;

/**
 * @author yxx
 * @since 2022-11-12 13:54
 */
public interface UserService extends IService<User> {
    /**
     * 登录
     *
     * @param request 请求参数
     * @return token等结果
     */
    LoginRes login(LoginReq request);

    /**
     * 注册
     *
     * @param req 用户注册信息
     * @return true-成功
     */
    Boolean register(UserRegisterReq req);
}
