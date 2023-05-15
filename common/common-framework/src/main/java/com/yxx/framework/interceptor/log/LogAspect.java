package com.yxx.framework.interceptor.log;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.tlog.context.TLogContext;
import com.yxx.common.annotation.log.OperationLog;
import com.yxx.common.constant.Constant;
import com.yxx.common.core.model.LogDTO;
import com.yxx.common.core.model.LoginUser;
import com.yxx.common.enums.LogTypeEnum;
import com.yxx.common.utils.IpUtil;
import com.yxx.common.utils.ServletUtils;
import com.yxx.framework.service.impl.OperationLogDefaultServiceImpl;
import com.yxx.framework.service.OperationLogService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * @author yxx
 * @since 2022/11/12 03:21
 */
@Aspect
@Component
@Slf4j
public class LogAspect {

    /**
     * 用来记录请求进入的时间，防止多线程时出错，这里用了ThreadLocal
     */
    ThreadLocal<Long> startTime = new ThreadLocal<>();

    private  OperationLogService logService;

    /**
     * 定义切入点，controller下面的所有类的所有公有方法
     */
    @Pointcut("execution(public * com.yxx..controller.*.*(..))")
    public void requestLog() {
        // document why this method is empty
    }

    /**
     * 方法之前执行，日志打印请求信息
     *
     * @param joinPoint joinPoint
     */
    @Before("requestLog()")
    public void doBefore(JoinPoint joinPoint) {
        startTime.set(System.currentTimeMillis());
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = servletRequestAttributes.getRequest();
        //打印当前的请求路径
        log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        log.info("RequestMapping:[{}]", request.getRequestURI());


        //这里是从token中获取用户信息，打印当前的访问用户，代码不通用
        if (StpUtil.isLogin()) {
            Object loginId = StpUtil.getLoginId();
            log.info("User is:" + loginId);
        }

        // 打印请求参数，如果需要打印其他的信息可以到request中去拿
        log.info("RequestParam:{}", Arrays.toString(joinPoint.getArgs()));
        log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
    }

    /**
     * 方法返回之前执行，打印才返回值以及方法消耗时间
     *
     * @param response 返回值
     */
    @AfterReturning(returning = "response", pointcut = "requestLog()")
    public void doAfterRunning(Object response) {
        try {
            //打印返回值信息
            log.info("++++++++++++++++++++++++++++++++++++++++++++");
            ObjectMapper jsonMapper = new ObjectMapper();
            log.info("Response:[{}]", jsonMapper.writeValueAsString(response));
            //打印请求耗时
            log.info("Request spend times : [{}ms]", System.currentTimeMillis() - startTime.get());
            log.info("++++++++++++++++++++++++++++++++++++++++++++");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            startTime.remove();
        }
    }

    public OperationLogService getLogService() {
        try {
            if (null == logService) {
                logService = SpringUtil.getBean(OperationLogService.class);
            }
        } catch (NoSuchBeanDefinitionException e) {
            log.warn("Please implement this OperationLogService interface");
            logService = new OperationLogDefaultServiceImpl();
        }
        return logService;
    }

    @Around("@annotation(operationLog)")
    @SneakyThrows
    public Object around(ProceedingJoinPoint point, OperationLog operationLog) {
        String strClassName = point.getTarget().getClass().getName();
        String strMethodName = point.getSignature().getName();
        log.info("[类名]:{},[方法]:{}", strClassName, strMethodName);

        // 方法开始时间
        Long beginTime = System.currentTimeMillis();
        LogTypeEnum type = LogTypeEnum.NORMAL;
        String exception = null;
        Object obj;
        try {
            obj = point.proceed();
        } catch (Exception e) {
            type = LogTypeEnum.ERROR;
            exception = e.getMessage();
            throw e;
        } finally {
            // 结束时间
            Long endTime = System.currentTimeMillis();
            Long time = endTime - beginTime;
            MethodSignature signature = (MethodSignature) point.getSignature();
            log.info("signature:[{}]", signature);
            String module = operationLog.module();
            String title = operationLog.title();
            String traceId = TLogContext.getTraceId();
            String spanId = TLogContext.getSpanId();
            LogDTO dto = createLog(module, title, type, time, traceId, spanId, exception);
            getLogService().saveLog(dto);
        }

        return obj;
    }

    private LogDTO createLog(String module, String title, LogTypeEnum logType, Long time,
                             String traceId, String spanId, String exception) {
        HttpServletRequest request = ServletUtils.getRequest();

        LogDTO log = new LogDTO();
        log.setModule(module);
        log.setTitle(title);
        log.setType(logType.getCode());

        String ip = IpUtil.getRequestIp();
        log.setIp(ip);
        log.setUserAgent(request.getHeader("user-agent"));
        log.setMethod(request.getMethod());
        log.setTime(time);
        log.setException(exception);

        if (StpUtil.isLogin()) {
            LoginUser loginUser = (LoginUser) StpUtil.getTokenSession().get(Constant.LOGIN_USER_KEY);
            log.setUserId(loginUser.getId());
            log.setCreateUid(loginUser.getId());
        }
        log.setParams(ServletUtils.getRequestParms(request));
        log.setRequestUri(request.getRequestURI());
        log.setTraceId(traceId);
        log.setSpanId(spanId);
        return log;
    }
}
