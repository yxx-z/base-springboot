package com.yxx.framework.interceptor.log;

import cn.dev33.satoken.stp.StpUtil;
import com.yxx.common.annotation.log.OperationLog;
import com.yxx.common.core.model.LogDTO;
import com.yxx.common.core.model.LoginUser;
import com.yxx.common.enums.LogTypeEnum;
import com.yxx.common.properties.IpProperties;
import com.yxx.common.utils.ServletUtils;
import com.yxx.common.utils.auth.LoginAdminUtils;
import com.yxx.common.utils.auth.LoginUtils;
import com.yxx.common.utils.ip.AddressUtil;
import com.yxx.common.utils.ip.IpUtil;
import com.yxx.common.utils.satoken.StpAdminUtil;
import com.yxx.framework.context.AppContext;
import com.yxx.framework.log.SensitiveDataSanitizer;
import com.yxx.framework.service.OperationLogService;
import com.yxx.framework.service.impl.OperationLogDefaultServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

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
public class LogAspect {

    private final ObjectProvider<OperationLogService> operationLogServiceProvider;
    private final SensitiveDataSanitizer sanitizer;
    private final IpProperties ipProperties;

    @Value("${app.name}")
    private String appName;

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

    /**
     * 保存带有 {@link OperationLog} 注解的业务操作审计记录。
     *
     * <p>审计保存异常只记录到服务端日志，不能覆盖业务方法原本的成功或失败结果。</p>
     *
     * @param point        业务连接点
     * @param operationLog 操作日志配置
     * @return 业务返回值
     * @throws Throwable 保持原始业务异常语义
     */
    @Around("@annotation(operationLog)")
    public Object recordOperation(ProceedingJoinPoint point, OperationLog operationLog) throws Throwable {
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
                operationLogService().saveLog(createLog(
                        operationLog, type, elapsedMillis(startNanos), exceptionMessage));
            } catch (Exception auditException) {
                log.error("保存操作审计日志失败，module={}，title={}",
                        operationLog.module(), operationLog.title(), auditException);
            }
        }
    }

    private LogDTO createLog(OperationLog operationLog, LogTypeEnum type, long time, String exceptionMessage) {
        HttpServletRequest request = ServletUtils.getRequest();
        LoginUser loginUser = currentLoginUser();

        LogDTO logDTO = new LogDTO();
        logDTO.setModule(operationLog.module());
        logDTO.setTitle(operationLog.title());
        logDTO.setType(type.getCode());
        logDTO.setIp(IpUtil.getRequestIp());
        if (Boolean.TRUE.equals(ipProperties.getCheck())) {
            logDTO.setIpHomePlace(AddressUtil.getIpHomePlace(logDTO.getIp(), 2));
        }
        logDTO.setUserAgent(request.getHeader("user-agent"));
        logDTO.setMethod(request.getMethod());
        logDTO.setTime(time);
        logDTO.setException(exceptionMessage);
        logDTO.setParams(sanitizer.sanitizeText(ServletUtils.getRequestParms(request)));
        logDTO.setRequestUri(request.getRequestURI());
        logDTO.setTraceId(AppContext.getTraceId());

        if (loginUser != null) {
            logDTO.setUserId(loginUser.getId());
            logDTO.setCreateUid(loginUser.getId());
        }
        return logDTO;
    }

    private LoginUser currentLoginUser() {
        if (StpAdminUtil.TYPE.equals(appName) && StpAdminUtil.isLogin()) {
            return LoginAdminUtils.getLoginUser();
        }
        if (StpUtil.isLogin()) {
            return LoginUtils.getLoginUser();
        }
        return null;
    }

    /**
     * 获取应用提供的审计持久化实现；未提供时降级为结构化日志输出。
     *
     * @return 操作日志服务
     */
    private OperationLogService operationLogService() {
        return operationLogServiceProvider.getIfAvailable(OperationLogDefaultServiceImpl::new);
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private String resultType(Object result) {
        return result == null ? "void" : result.getClass().getSimpleName();
    }
}
