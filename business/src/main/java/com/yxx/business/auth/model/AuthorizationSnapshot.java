package com.yxx.business.auth.model;

import java.util.Set;

/**
 * 登录时加载的用户端授权快照。
 *
 * @param roles       角色编码集合
 * @param permissions 后端权限编码集合
 */
public record AuthorizationSnapshot(Set<String> roles, Set<String> permissions) {
}
