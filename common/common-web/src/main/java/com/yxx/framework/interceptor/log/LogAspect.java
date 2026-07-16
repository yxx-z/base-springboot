package com.yxx.framework.interceptor.log;

import com.yxx.common.utils.ServletUtils;
import com.yxx.framework.log.SensitiveDataSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * HTTP 访问日志与业务操作审计切面。
 *
 * <p>访问日志只记录定位问题所需的元数据和脱敏参数，不再打印完整响应对象，
 * 避免密码、Token、支付信息和大对象进入日志系统。</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "web.access-log", name = "enabled", havingValue = "true")
public class LogAspect {

    private final SensitiveDataSanitizer sanitizer;

    /** Controller 公共方法切点。 */
    @Pointcut("execution(public * com.yxx..controller.*.*(..))")
    public void controllerMethod() {
        // 切点声明方法无需实现。
    }

    /**
     * 记录一次 HTTP 请求的入口、执行结果和耗时。
     *
     * @param point Controller 连接点
     * @return Controller 返回值
     * @throws Throwable 保持原始业务异常语义
     */
    @Around("controllerMethod()")
    public Object recordRequest(ProceedingJoinPoint point) throws Throwable {
        HttpServletRequest request = ServletUtils.getRequest();
        long startNanos = System.nanoTime();
        log.info("请求开始 method={} uri={} handler={} params={}",
                request.getMethod(), request.getRequestURI(), point.getSignature().toShortString(),
                sanitizer.sanitizeArguments(point.getArgs()));
        try {
            Object result = point.proceed();
            log.info("请求完成 method={} uri={} resultType={} durationMs={}",
                    request.getMethod(), request.getRequestURI(), resultType(result), elapsedMillis(startNanos));
            return result;
        } catch (Throwable throwable) {
            log.warn("请求失败 method={} uri={} exceptionType={} durationMs={}",
                    request.getMethod(), request.getRequestURI(), throwable.getClass().getSimpleName(),
                    elapsedMillis(startNanos));
            throw throwable;
        }
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private String resultType(Object result) {
        return result == null ? "void" : result.getClass().getSimpleName();
    }
}
