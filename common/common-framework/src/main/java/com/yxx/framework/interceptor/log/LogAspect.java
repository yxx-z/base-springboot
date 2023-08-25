package com.yxx.framework.interceptor.log;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yxx.common.annotation.log.OperationLog;
import com.yxx.common.constant.Constant;
import com.yxx.common.constant.EmailSubjectConstant;
import com.yxx.common.constant.RedisConstant;
import com.yxx.common.core.model.LogDTO;
import com.yxx.common.core.model.LoginUser;
import com.yxx.common.enums.LogTypeEnum;
import com.yxx.common.properties.IpProperties;
import com.yxx.common.properties.MailProperties;
import com.yxx.common.properties.MyWebProperties;
import com.yxx.common.utils.ServletUtils;
import com.yxx.common.utils.agent.UserAgentUtil;
import com.yxx.common.utils.auth.LoginUtils;
import com.yxx.common.utils.email.MailUtils;
import com.yxx.common.utils.ip.AddressUtil;
import com.yxx.common.utils.ip.IpUtil;
import com.yxx.common.utils.redis.RedissonCache;
import com.yxx.framework.service.OperationLogService;
import com.yxx.framework.service.impl.OperationLogDefaultServiceImpl;
import lombok.RequiredArgsConstructor;
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

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * @author yxx
 * @since 2022/11/12 03:21
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {
    private final MailUtils mailUtils;

    private final MailProperties mailProperties;

    private final RedissonCache redissonCache;

    private final IpProperties ipProperties;

    private final MyWebProperties myWebProperties;

    /**
     * 用来记录请求进入的时间，防止多线程时出错，这里用了ThreadLocal
     */
    ThreadLocal<Long> startTime = new ThreadLocal<>();

    private OperationLogService logService;

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
        // 获取请求ip
        String requestIp = IpUtil.getRequestIp();
        // 判断是否登录
        boolean isLogin = StpUtil.isLogin();
        if (isLogin) {
            LoginUser loginUser = LoginUtils.getLoginUser();
            log.info("异步前");
            // 获取agent
            String requestAgent = request.getHeader("user-agent");
            CompletableFuture.runAsync(() -> checkIpUnusual(requestIp, loginUser, requestAgent));
            log.info("异步后");
        }
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

            // todo 去掉tlog
            String traceId = null;
            String spanId = null;
            LogDTO dto = createLog(module, title, type, time, traceId, spanId, exception);
            getLogService().saveLog(dto);
        }

        return obj;
    }

    private LogDTO createLog(String module, String title, LogTypeEnum logType, Long time,
                             String traceId, String spanId, String exception) {
        HttpServletRequest request = ServletUtils.getRequest();

        LogDTO logDTO = new LogDTO();
        logDTO.setModule(module);
        logDTO.setTitle(title);
        logDTO.setType(logType.getCode());

        String ip = IpUtil.getRequestIp();
        log.info("当前IP为{}", ip);
        if (Boolean.TRUE.equals(ipProperties.getCheck())) {
            String ipHomePlace = AddressUtil.getIpHomePlace(ip, 2);
            logDTO.setIpHomePlace(ipHomePlace);
        }
        logDTO.setIp(ip);
        logDTO.setUserAgent(request.getHeader("user-agent"));
        logDTO.setMethod(request.getMethod());
        logDTO.setTime(time);
        logDTO.setException(exception);

        if (StpUtil.isLogin()) {
            LoginUser loginUser = (LoginUser) StpUtil.getTokenSession().get(Constant.LOGIN_USER_KEY);
            logDTO.setUserId(loginUser.getId());
            logDTO.setCreateUid(loginUser.getId());
        }
        logDTO.setParams(ServletUtils.getRequestParms(request));
        logDTO.setRequestUri(request.getRequestURI());
        logDTO.setTraceId(traceId);
        logDTO.setSpanId(spanId);
        return logDTO;
    }

    /**
     * 检查ip是否异常
     *
     * @param requestIp 请求ip
     * @author yxx
     */
    private void checkIpUnusual(String requestIp, LoginUser loginUser, String requestAgent) {
        if (Boolean.TRUE.equals(ipProperties.getCheck())) {
            log.info("开始校验ip是否异常~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            // 如果是有效ip 进行校验
            if (AddressUtil.isValidIPv4(requestIp) || AddressUtil.isIPv6Address(requestIp)) {
                // 判断ip是否是ipv6
                boolean iPv6Address = AddressUtil.isIPv6Address(requestIp);
                if (iPv6Address) {
                    log.info("该ip：{},为ipv6暂不解析", requestIp);
                }
                // 如果已经登录 并且是ipv4 则判断ip是否异常
                if (AddressUtil.isValidIPv4(requestIp)) {
                    log.info("校验ipv4");
                    // 获取登录时ip属地
                    String ipHomePlace = loginUser.getIpHomePlace();
                    // 登录时设备名称
                    String loginAgent = loginUser.getAgent();
                    // 判断是否发送过ip异常邮件 如果没发送过，进行ip异常校验
                    boolean exists = redissonCache.exists(RedisConstant.IP_UNUSUAL_OPERATE + loginUser.getId());
                    if (!exists) {
                        // 当前操作的ip
                        String currentIpHomePlace = AddressUtil.getIpHomePlace(requestIp, 2);
                        String agent = UserAgentUtil.getAgent(requestAgent);
                        // 判断当前ip归属地和登录时ip归属地是否一致，如果不一致，发送邮件告知用户
                        if (!currentIpHomePlace.equals(ipHomePlace) && !loginAgent.equals(agent)) {
                            log.info("校验完成，ip行为异常");
                            // 邮件正文
                            String unusual = AddressUtil.getIpHomePlace(requestIp, 3);
                            String time = LocalDateTimeUtil.format(LocalDateTime.now(), DatePattern.NORM_DATETIME_PATTERN);
                            String emailContent = mailProperties.getIpUnusualContent().replace("{time}", time)
                                    .replace("{ip}", requestIp)
                                    .replace("{address}", unusual)
                                    .replace("{agent}", agent)
                                    .replace("{formName}", mailProperties.getFromName())
                                    .replace("{form}", mailProperties.getFrom()
                                    .replace("{domain}", myWebProperties.getDomain()));
                            // 发送邮件
                            mailUtils.baseSendMail(loginUser.getEmail(), EmailSubjectConstant.IP_UNUSUAL, emailContent, true);
                            // 加入redis(一天提醒一次)
                            // 今天剩余时间
                            Long residueTime = theRestOfTheDaySecond();
                            redissonCache.put(RedisConstant.IP_UNUSUAL_OPERATE + loginUser.getId(), Boolean.TRUE, residueTime);
                        }
                    }
                }
            } else {
                log.info("ip地址：{} 无法解析", requestIp);
            }
            log.info("校验ip是否异常结束~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        }
    }

    /**
     * 今天剩余时间(单位秒)
     *
     * @return {@link Long }
     * @author yxx
     */
    public static Long theRestOfTheDaySecond() {
        // 获取当前日期和时间
        LocalDateTime now = LocalDateTime.now();

        // 获取今晚的十二点整时间
        LocalDateTime midnight = now.toLocalDate().atTime(LocalTime.MAX);

        // 计算当前时间距离今晚十二点整的秒数
        Duration duration = Duration.between(now, midnight);
        return duration.getSeconds();
    }
}
