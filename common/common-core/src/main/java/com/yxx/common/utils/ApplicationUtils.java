package com.yxx.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

/**
 * Spring Application 工具类
 *
 * @author yxx
 * @since 2021/12/29 10:43
 */
@Slf4j
@Lazy(false)
@Component
public class ApplicationUtils implements ApplicationContextAware, DisposableBean {

    /**
     * 全局的ApplicationContext
     */
    private static ApplicationContext applicationContext = null;

    /**
     * 获取ApplicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 获取springbean
     */
    public static <T> T getBean(String beanName, Class<T> requiredType) {
        if (containsBean(beanName)) {
            return applicationContext.getBean(beanName, requiredType);
        }
        return null;
    }

    /**
     * 获取springbean
     */
    public static <T> T getBean(Class<T> requiredType) {
        return applicationContext.getBean(requiredType);
    }

    /**
     * 获取springbean
     */
    public static <T> T getBean(String beanName) {
        if (containsBean(beanName)) {
            Class<T> type = getType(beanName);
            return applicationContext.getBean(beanName, type);
        }
        return null;
    }

    /**
     * 依赖spring框架获取HttpServletRequest
     *
     * @return HttpServletRequest
     */
    public static HttpServletRequest getRequest() {
        HttpServletRequest request = null;
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (Objects.nonNull(requestAttributes)) {
                request = requestAttributes.getRequest();
            }
        } catch (Exception ignored) {
        }
        return request;
    }

    public static HttpServletResponse getResponse() {
        HttpServletResponse response = null;
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (Objects.nonNull(requestAttributes)) {
                response = requestAttributes.getResponse();
            }
        } catch (Exception ignored) {
        }
        return response;
    }

    /**
     * 发布事件
     */
    public static void publishEvent(ApplicationEvent event) {
        if (applicationContext == null) {
            return;
        }
        applicationContext.publishEvent(event);
    }

    /**
     * ApplicationContext是否包含该Bean
     */
    public static boolean containsBean(String name) {
        return applicationContext.containsBean(name);
    }

    /**
     * ApplicationContext该Bean是否为单例
     */
    public static boolean isSingleton(String name) {
        return applicationContext.isSingleton(name);
    }

    /**
     * 获取该Bean的Class
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getType(String name) {
        return (Class<T>) applicationContext.getType(name);
    }

    @Override
    public void destroy() {
        ApplicationUtils.clearHolder();
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        ApplicationUtils.applicationContext = applicationContext;
    }

    /**
     * 清除ApplicationUtils中的ApplicationContext为Null.
     */
    public static void clearHolder() {
        log.debug("清除ApplicationUtils中的ApplicationContext:" + applicationContext);
        applicationContext = null;
    }
}
