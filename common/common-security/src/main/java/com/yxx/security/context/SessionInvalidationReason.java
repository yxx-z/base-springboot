package com.yxx.security.context;

/**
 * 会话失效原因。
 *
 * <p>原因会写入结构化日志和重试耗尽事件，便于区分密码凭据变化、账号状态变化与
 * RBAC 授权变化。该枚举只描述安全语义，不包含具体业务模块信息。</p>
 */
public enum SessionInvalidationReason {

    /** 用户主动修改密码。 */
    PASSWORD_CHANGED,

    /** 通过找回密码流程重置密码。 */
    PASSWORD_RESET,

    /** 账号启用或停用状态发生变化。 */
    ACCOUNT_STATUS_CHANGED,

    /** 账号被注销或软删除。 */
    ACCOUNT_DELETED,

    /** 主体拥有的角色集合发生变化。 */
    SUBJECT_ROLE_CHANGED,

    /** 角色包含的后端权限发生变化。 */
    ROLE_PERMISSION_CHANGED,

    /** 角色被删除，主体原有授权关系失效。 */
    ROLE_DELETED,

    /** 权限被修改或删除，主体原有授权快照失效。 */
    PERMISSION_CHANGED
}
