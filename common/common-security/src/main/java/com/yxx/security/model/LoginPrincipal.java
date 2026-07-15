package com.yxx.security.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 当前登录会话中的统一认证主体。
 *
 * <p>该对象是运行时安全快照，不是数据库实体，也不应直接作为接口响应返回。密码、支付宝、
 * 微信等不同登录方式最终都必须转换为该模型，使鉴权、审计和数据填充只依赖稳定的内部用户
 * 标识，而不需要理解第三方平台的账号结构。</p>
 */
@Value
@Builder
@Jacksonized
public class LoginPrincipal implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 系统内部稳定主体标识，对应用户表或管理员表主键。 */
    Long subjectId;

    /** 主体类型，例如 {@code user}、{@code admin}。 */
    String subjectType;

    /** 系统账号展示标识；第三方平台的外部用户编号不得直接作为系统账号暴露。 */
    String account;

    /** 当前主体的显示名称。 */
    String displayName;

    /** 本次登录方式，例如 {@code password}、{@code alipay}。 */
    String loginMode;

    /** 登录时加载的角色编码快照。 */
    @Builder.Default
    Set<String> roles = Collections.emptySet();

    /** 登录时加载的后端权限编码快照。 */
    @Builder.Default
    Set<String> permissions = Collections.emptySet();

    /** 本次会话建立时间。 */
    LocalDateTime loginTime;

    /**
     * 返回不可修改且保持查询顺序的角色集合，防止业务代码意外修改 Session 权限快照。
     *
     * @return 不可修改角色集合
     */
    public Set<String> getRoles() {
        return immutableSet(roles);
    }

    /**
     * 返回不可修改且保持查询顺序的权限集合。
     *
     * @return 不可修改权限集合
     */
    public Set<String> getPermissions() {
        return immutableSet(permissions);
    }

    private Set<String> immutableSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }
}
