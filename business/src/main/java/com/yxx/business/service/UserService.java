package com.yxx.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxx.business.model.entity.User;
import com.yxx.business.model.request.*;
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

    /**
     * 发送重置密码邮件
     *
     * @param req 要求事情
     * @return {@link Boolean }
     * @author yxx
     */
    Boolean resetPwdEmail(ResetPwdEmailReq req);

    /**
     * 重置密码
     *
     * @param req 要求事情
     * @return {@link Boolean }
     * @author yxx
     */
    Boolean resetPwd(ResetPwdReq req);

    /**
     * 根据电子邮件获取用户
     *
     * @param email 电子邮件
     * @return {@link User }
     * @author yxx
     */
    User getUserByEmail(String email);


    /**
     * 修改密码
     *
     * @param req 要求事情
     * @return {@link Boolean }
     * @author yxx
     */
    Boolean editPwd(EditPwdReq req);

    /**
     * 发送注册验证码
     *
     * @param req 要求事情
     * @return {@link Boolean }
     * @author yxx
     */
    Boolean sendRegisterCaptcha(RegisterCaptchaReq req);
}
