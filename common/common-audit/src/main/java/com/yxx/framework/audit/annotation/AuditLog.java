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
}
