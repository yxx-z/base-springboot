package com.yxx.business.auth.model;

import com.yxx.business.model.entity.User;

/**
 * 认证策略输出的统一用户结果。
 *
 * @param user      已通过认证的系统用户
 * @param account   用于会话展示的系统账号
 * @param loginMode 本次登录方式
 */
public record AuthenticatedUser(User user, String account, String loginMode) {
}
