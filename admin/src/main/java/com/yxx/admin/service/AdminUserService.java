package com.yxx.admin.service;

import com.yxx.admin.model.entity.AdminUser;
import com.yxx.admin.model.request.EditPwdReq;
import com.yxx.admin.model.request.LoginReq;
import com.yxx.admin.model.request.ResetPwdEmailReq;
import com.yxx.admin.model.request.ResetPwdReq;
import com.yxx.admin.model.response.LoginRes;

/**
 * @author yxx
 * @since 2022-11-12 13:54
 */
public interface AdminUserService {

    /** 查询管理员主体，不向调用方暴露通用 CRUD。 */
    AdminUser findById(Long userId);

    /** 更新登录元数据。 */
    boolean updateLoginMetadata(Long userId, String agent, String ipHomePlace);

    /** 启用或停用管理员；停用最后一个超级管理员会被拒绝。 */
    void changeStatus(Long userId, boolean enabled);

    /** 删除管理员；删除最后一个超级管理员会被拒绝。 */
    void delete(Long userId);

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
    void resetPwdEmail(ResetPwdEmailReq req);

    /**
     * 重置密码
     *
     * @param req 要求事情
     * @return {@link Boolean }
     * @author yxx
     */
    void resetPwd(ResetPwdReq req);

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
    void editPwd(EditPwdReq req);
}
