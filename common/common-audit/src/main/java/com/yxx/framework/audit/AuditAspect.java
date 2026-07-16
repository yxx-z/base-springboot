package com.yxx.framework.audit;

import com.yxx.common.enums.LogTypeEnum;
import com.yxx.common.properties.IpProperties;
import com.yxx.common.utils.ServletUtils;
import com.yxx.common.utils.ip.AddressUtil;
import com.yxx.common.utils.ip.ClientIpResolver;
import com.yxx.framework.audit.annotation.AuditLog;
import com.yxx.framework.audit.model.AuditEvent;
import com.yxx.framework.context.AppContext;
import com.yxx.framework.log.SensitiveDataSanitizer;
import com.yxx.security.context.CurrentActorProvider;
import com.yxx.security.model.CurrentActor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.beans.BeanWrapperImpl;

/**
 * 声明式操作审计切面。
 *
 * <p>切面在业务方法执行前保存操作人快照，因此注销操作即使在目标方法中清除了 Session，
 * 仍能正确记录操作者。切面只发布事件，不依赖具体数据库表和业务用户模型。</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final CurrentActorProvider currentActorProvider;
    private final AuditEventPublisher auditEventPublisher;
    private final SensitiveDataSanitizer sanitizer;
    private final IpProperties ipProperties;
    private final ClientIpResolver clientIpResolver;
    private final AddressUtil addressUtil;

    @Around("@annotation(auditLog)")
    public Object audit(ProceedingJoinPoint point, AuditLog auditLog) throws Throwable {
        HttpServletRequest request = ServletUtils.getRequest();
        CurrentActor actorSnapshot = currentActorProvider.currentActor().orElse(null);
        long startNanos = System.nanoTime();
        LogTypeEnum type = LogTypeEnum.NORMAL;
        String exceptionMessage = null;
        try {
            return point.proceed();
        } catch (Throwable throwable) {
            type = LogTypeEnum.ERROR;
            exceptionMessage = sanitizer.sanitizeText(throwable.getMessage());
            throw throwable;
        } finally {
            try {
                // 登录接口执行前没有操作人，成功后再读取一次；注销接口执行后会清空 Session，
                // 因此优先使用执行前快照。两类审计场景都能获得正确主体。
                CurrentActor eventActor = actorSnapshot != null
                        ? actorSnapshot
                        : currentActorProvider.currentActor().orElse(null);
                auditEventPublisher.publish(createEvent(
                        request, point.getArgs(), eventActor, auditLog,
                        type, startNanos, exceptionMessage));
            } catch (RuntimeException publishException) {
                // 审计失败不能覆盖业务方法原本的成功或异常语义。
                log.error("发布操作审计事件失败，module={}，action={}",
                        auditLog.module(), auditLog.action(), publishException);
            }
        }
    }

    private AuditEvent createEvent(HttpServletRequest request,
                                   Object[] arguments,
                                   CurrentActor actor,
                                   AuditLog auditLog,
                                   LogTypeEnum type,
                                   long startNanos,
                                   String exceptionMessage) {
        String ip = clientIpResolver.resolve(request);
        String ipRegion = Boolean.TRUE.equals(ipProperties.getCheck())
                ? addressUtil.getIpHomePlace(ip, 2)
                : null;
        String params = auditLog.recordRequest()
                ? sanitizer.sanitizeText(ServletUtils.getRequestParms(request))
                : "";
        return new AuditEvent(
                actor,
                auditLog.eventType(),
                auditLog.module(),
                auditLog.action(),
                auditLog.resource(),
                type.getCode(),
                ip,
                ipRegion,
                request.getHeader("user-agent"),
                request.getRequestURI(),
                request.getMethod(),
                params,
                resolveSubjectAccount(arguments, auditLog.subjectField()),
                AppContext.getTraceId(),
                (System.nanoTime() - startNanos) / 1_000_000L,
                exceptionMessage);
    }

    /**
     * 从请求对象中只提取明确声明的账号字段。该方法不会序列化完整参数，适用于失败登录等
     * 尚未建立 CurrentActor、同时又不能记录密码的安全审计场景。
     */
    private String resolveSubjectAccount(Object[] arguments, String subjectField) {
        if (subjectField == null || subjectField.isBlank() || arguments == null) {
            return null;
        }
        for (Object argument : arguments) {
            if (argument == null) {
                continue;
            }
            BeanWrapperImpl wrapper = new BeanWrapperImpl(argument);
            if (wrapper.isReadableProperty(subjectField)) {
                Object value = wrapper.getPropertyValue(subjectField);
                return value == null ? null : sanitizer.sanitizeText(String.valueOf(value));
            }
        }
        return null;
    }
}
