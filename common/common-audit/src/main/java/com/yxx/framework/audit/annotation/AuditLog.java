package com.yxx.framework.audit.annotation;

import com.yxx.framework.audit.model.AuditEventType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式业务操作审计注解。
 *
 * <p>注解只描述审计语义，具体操作人解析、请求上下文采集和事件存储由审计基础设施完成。</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /** 业务模块名称。 */
    String module();

    /** 结构化操作名称。 */
    String action();

    /** 结构化事件分类。 */
    AuditEventType eventType() default AuditEventType.OPERATION;

    /** 资源类型或资源说明。 */
    String resource() default "";

    /** 是否记录经过脱敏的请求参数。登录、重置密码等敏感接口应关闭。 */
    boolean recordRequest() default true;

    /** 被操作主体类型，例如 {@code business-user}；为空表示本次操作没有明确业务主体。 */
    String subjectType() default "";

    /**
     * 被操作主体稳定标识的 SpEL 表达式，例如 {@code #userId}。
     *
     * <p>只解析显式声明的表达式，不会序列化完整请求参数。</p>
     */
    String subjectId() default "";

    /**
     * 被操作或尝试登录账号的 SpEL 表达式，例如 {@code #request.loginCode}。
     * 登录、注册等匿名接口可用它记录账号，同时避免记录密码和验证码。</p>
     */
    String subjectAccount() default "";
}
