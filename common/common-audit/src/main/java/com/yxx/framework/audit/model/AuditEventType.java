package com.yxx.framework.audit.model;

/** 审计事件分类，供查询、告警和统计使用，禁止依赖中文模块名称判断事件语义。 */
public enum AuditEventType {
    /** 身份认证相关事件，例如登录和退出。 */
    AUTHENTICATION,

    /** 普通业务操作事件。 */
    OPERATION,

    /** 安全敏感事件，例如修改凭据、调整权限。 */
    SECURITY
}
