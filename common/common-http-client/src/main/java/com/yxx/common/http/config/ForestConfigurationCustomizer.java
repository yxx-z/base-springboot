package com.yxx.common.http.config;

import com.dtflys.forest.config.ForestConfiguration;
import com.yxx.common.http.interceptor.ForestTraceIdInterceptor;
import com.yxx.common.http.properties.FrameworkHttpClientProperties;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;

import java.util.ArrayList;

/**
 * Forest 运行时配置后处理器。
 *
 * <p>Forest Starter 会把 {@code forest.*} 属性复制到运行时 {@link ForestConfiguration}，
 * 并立即使用该对象创建 Client。此处理器直接定制最终运行时配置，确保日志安全策略和全局拦截器
 * 在任何出站请求执行前生效。</p>
 */
public class ForestConfigurationCustomizer implements BeanPostProcessor, EnvironmentAware, PriorityOrdered {

    private Environment environment;

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    /**
     * Forest 的 {@code ForestBeanRegister} 本身也是 BeanPostProcessor，并会在创建过程中立即构造
     * {@link ForestConfiguration}。本处理器必须优先注册，才能处理该阶段动态创建的配置 Bean。
     *
     * @return 最高优先级
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) {
        if (!(bean instanceof ForestConfiguration forestConfiguration)) {
            return bean;
        }

        FrameworkHttpClientProperties properties = Binder.get(environment)
                .bind("framework.http-client", Bindable.of(FrameworkHttpClientProperties.class))
                .orElseGet(FrameworkHttpClientProperties::new);
        if (properties.isTraceIdPropagationEnabled()) {
            addTraceIdInterceptor(forestConfiguration);
        }
        applyLoggingPolicy(forestConfiguration, properties.getLogging());
        return bean;
    }

    private void addTraceIdInterceptor(ForestConfiguration forestConfiguration) {
        if (forestConfiguration.getInterceptors() == null) {
            forestConfiguration.setInterceptors(new ArrayList<>());
        }
        // Forest 1.8 的运行时配置集合仍保留旧 Interceptor 泛型以兼容历史版本，
        // 此处不直接依赖该弃用接口；实际注册类实现的是新版 ForestInterceptor。
        var interceptors = forestConfiguration.getInterceptors();
        if (!interceptors.contains(ForestTraceIdInterceptor.class)) {
            interceptors.add(ForestTraceIdInterceptor.class);
        }
    }

    private void applyLoggingPolicy(ForestConfiguration forestConfiguration,
                                    FrameworkHttpClientProperties.Logging logging) {
        // 基础框架默认完全关闭 Forest 日志，防止 URL 查询参数、认证头和请求体被意外打印。
        // 确需排障时由应用显式开启，并逐项决定可以记录的内容。
        forestConfiguration.setLogEnabled(logging.isEnabled());
        forestConfiguration.setLogRequest(logging.isRequest());
        forestConfiguration.setLogRequestHeaders(logging.isRequestHeaders());
        forestConfiguration.setLogRequestBody(logging.isRequestBody());
        forestConfiguration.setLogResponseStatus(logging.isResponseStatus());
        forestConfiguration.setLogResponseHeaders(logging.isResponseHeaders());
        forestConfiguration.setLogResponseContent(logging.isResponseContent());
    }
}
