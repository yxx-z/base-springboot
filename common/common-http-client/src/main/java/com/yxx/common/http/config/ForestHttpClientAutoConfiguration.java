package com.yxx.common.http.config;

import com.dtflys.forest.springboot.ForestAutoConfiguration;
import com.dtflys.forest.springboot.properties.ForestConfigurationProperties;
import com.yxx.common.http.interceptor.ForestTraceIdInterceptor;
import com.yxx.common.http.properties.FrameworkHttpClientProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Forest HTTP 客户端公共自动配置。
 *
 * <p>该模块只补充跨应用稳定的链路标识和日志安全策略，不扫描或声明任何第三方平台 Client。
 * 具体接口、DTO、认证签名、业务错误码和重试语义必须留在实际调用方模块。</p>
 */
@AutoConfiguration(before = ForestAutoConfiguration.class)
@ConditionalOnClass(ForestConfigurationProperties.class)
@EnableConfigurationProperties(FrameworkHttpClientProperties.class)
public class ForestHttpClientAutoConfiguration {

    /**
     * 注册 TraceId 透传拦截器。
     *
     * @param properties 公共 HTTP 客户端配置
     * @return Forest TraceId 拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    public ForestTraceIdInterceptor forestTraceIdInterceptor(FrameworkHttpClientProperties properties) {
        return new ForestTraceIdInterceptor(properties.getTraceIdHeaderName());
    }

    /**
     * 在 Forest 创建全局配置前应用公共安全策略。
     *
     * <p>Forest 原生默认会打印请求头和请求体，不适合作为基础框架的默认值。本定制器通过
     * {@code framework.http-client.logging} 显式管理日志开关，并将 TraceId 拦截器加入全局链路。</p>
     *
     * @return Forest 配置定制器
     */
    @Bean
    public static ForestConfigurationCustomizer forestConfigurationCustomizer() {
        // BeanPostProcessor 必须由静态工厂方法尽早注册，避免自动配置类及其依赖提前实例化，
        // 从而错过其他 BeanPostProcessor 的标准处理流程。
        return new ForestConfigurationCustomizer();
    }
}
