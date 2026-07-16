package com.yxx.security.authorization;

import com.yxx.security.model.AuthorizationSnapshot;

/**
 * 授权信息提供器。
 *
 * <p>安全模块只约定“根据认证域和主体主键加载授权快照”，不感知角色、权限来自数据库、
 * 远程权限中心还是配置文件。具体项目可以使用 RBAC，也可以替换为其他授权模型。</p>
 */
@FunctionalInterface
public interface AuthorizationProvider {

    /**
     * 加载指定登录主体的角色和权限。
     *
     * @param realm     认证域，例如 {@code user} 或 {@code admin}
     * @param subjectId 登录主体的稳定数据库主键
     * @return 不为 {@code null} 的授权快照；没有授权时返回空集合快照
     */
    AuthorizationSnapshot load(String realm, Long subjectId);
}
