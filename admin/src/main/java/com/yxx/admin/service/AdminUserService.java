package com.yxx.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxx.admin.model.entity.AdminUser;
import com.yxx.admin.model.request.*;
import com.yxx.admin.model.response.LoginRes;

/**
 * @author yxx
 * @since 2022-11-12 13:54
 */
public interface AdminUserService extends IService<AdminUser> {
    /**
     * 登录
     *
     * @param request 请求参数
     * @return token等结果
     */
    LoginRes login(LoginReq request);

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
     * @return {@link AdminUser }
     * @author yxx
     */
    AdminUser getUserByEmail(String email);

    /**
     * 修改密码
     *
     * @param req 要求事情
     * @return {@link Boolean }
     * @author yxx
     */
    Boolean editPwd(EditPwdReq req);
}
