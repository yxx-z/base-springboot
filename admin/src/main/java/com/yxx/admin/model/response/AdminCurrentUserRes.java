package com.yxx.admin.model.response;

import java.util.Set;

/**
 * 当前管理员信息响应。
 *
 * @param id          管理员主键
 * @param account     登录账号
 * @param displayName 显示名称
 * @param phone       联系手机
 * @param email       联系邮箱
 * @param roles       当前角色编码
 * @param permissions 当前后端权限编码
 */
public record AdminCurrentUserRes(
        Long id,
        String account,
        String displayName,
        String phone,
        String email,
        Set<String> roles,
        Set<String> permissions) {
}
