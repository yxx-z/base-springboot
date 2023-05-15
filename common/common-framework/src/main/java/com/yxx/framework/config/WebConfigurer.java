package com.yxx.framework.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import com.yxx.framework.interceptor.response.ResponseResultInterceptor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 注册拦截器
 *
 * @author zhanglf
 * @since 2022/11/12 03:21
 */
@Configuration
public class WebConfigurer implements WebMvcConfigurer {


    private final ResponseResultInterceptor interceptor;

    @Autowired(required = false)
    public WebConfigurer(ResponseResultInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    /**
     * Sa-Token 整合 jwt (Style模式)
     *
     * @return 权限认证，逻辑实现类
     */
    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }

    /**
     * 用来注册拦截器，拦截器需要通过这里添加注册才能生效
     *
     * @param registry 拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // addPathPatterns("/**") 表示拦截所有的请求，
        // excludePathPatterns("/login", "/register") 表示除了登录与注册之外，因为登录注册不需要登录也可以访问
        registry.addInterceptor(interceptor).addPathPatterns("/**");
        // 注册注解拦截器，并排除不需要注解鉴权的接口地址 (与登录拦截器无关)
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 删除springboot默认的StringHttpMessageConverter解析器
        // 不删除的话，ResponseResultHandler类的beforeBodyWrite方法解析封装String时会出现转换异常
        converters.removeIf(StringHttpMessageConverter.class::isInstance);
    }

    /**
     * 配置静态资源，比如html，js，css，等等
     *
     * @param registry registry
     */
    @Override
    public void addResourceHandlers(@NotNull ResourceHandlerRegistry registry) {
        // 配置静态资源，比如html，js，css，等等
    }

    /**
     * 跨域配置
     *
     * @param registry 跨域注册表
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // SpringBoot2.4.0 [allowedOriginPatterns]代替[allowedOrigins]
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .maxAge(3600)
                .allowCredentials(true);
    }
}