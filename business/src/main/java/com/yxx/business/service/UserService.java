package com.yxx.business.service;

import com.yxx.business.model.entity.User;
import com.yxx.business.model.request.EditPwdReq;
import com.yxx.business.model.request.RegisterCaptchaReq;
import com.yxx.business.model.request.ResetPwdEmailReq;
import com.yxx.business.model.request.ResetPwdReq;
import com.yxx.business.model.request.UserRegisterReq;

/**
 * @author yxx
 * @since 2022-11-12 13:54
 */
public interface UserService {
    /** 查询用户主体，仅供业务应用服务读取。 */
    User findById(Long userId);

    /** 创建用户主体，统一保留领域校验入口。 */
    boolean create(User user);

    /** 更新登录元数据，不暴露通用 updateById。 */
    boolean updateLoginMetadata(Long userId, String agent, String ipHomePlace);

    /** 启用或停用用户；停用后在事务提交后注销全部会话。 */
    void changeStatus(Long userId, boolean enabled);

    /** 删除用户；删除后在事务提交后注销全部会话。 */
    void delete(Long userId);

    /**
     * 注册
     *
     * @param req 用户注册信息
     */
    void register(UserRegisterReq req);

    /**
     * 发送重置密码邮件
     *
     * @param req 要求事情
     * @author yxx
     */
    void resetPwdEmail(ResetPwdEmailReq req);

    /**
     * 重置密码
     *
     * @param req 要求事情
     * @author yxx
     */
    void resetPwd(ResetPwdReq req);

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
     * @author yxx
     */
    void editPwd(EditPwdReq req);

    /**
     * 发送注册验证码
     *
     * @param req 要求事情
     * @author yxx
     */
    void sendRegisterCaptcha(RegisterCaptchaReq req);
}
