package com.yxx.business.model.response;

import java.util.Set;

/**
 * 当前用户信息响应。
 *
 * <p>接口响应与安全 Session 分离，避免 Token、第三方身份标识或内部会话字段被直接返回。</p>
 *
 * @param id          用户主键
 * @param account     系统账号展示标识
 * @param displayName 用户显示名称
 * @param avatar      用户头像
 * @param phone       联系手机
 * @param email       联系邮箱
 * @param roles       当前角色编码
 * @param permissions 当前后端权限编码
 */
public record CurrentUserRes(
        Long id,
        String account,
        String displayName,
        String avatar,
        String phone,
        String email,
        Set<String> roles,
        Set<String> permissions) {
}
