package com.yxx.security.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 登录主体的授权快照。
 *
 * <p>构造时执行防御性复制，避免调用方在快照写入 Session 后继续修改原始集合。集合保留
 * 稳定迭代顺序，便于日志、接口响应和自动化测试保持一致。</p>
 *
 * @param roles       角色编码集合
 * @param permissions 后端权限编码集合
 */
public record AuthorizationSnapshot(Set<String> roles, Set<String> permissions) {

    public AuthorizationSnapshot {
        roles = immutableSet(roles);
        permissions = immutableSet(permissions);
    }

    /** 返回一个没有任何角色和权限的快照。 */
    public static AuthorizationSnapshot empty() {
        return new AuthorizationSnapshot(Collections.emptySet(), Collections.emptySet());
    }

    private static Set<String> immutableSet(Set<String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }
}
