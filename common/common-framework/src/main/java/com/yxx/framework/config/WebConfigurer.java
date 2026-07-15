package com.yxx.framework.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import com.yxx.framework.interceptor.response.ResponseResultInterceptor;
import com.yxx.framework.interceptor.security.AuthenticationInterceptor;
import com.yxx.framework.config.properties.CorsProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册拦截器
 *
 * @author zhanglf
 * @since 2022/11/12 03:21
 */
@Configuration
public class WebConfigurer implements WebMvcConfigurer {


    private final ResponseResultInterceptor interceptor;
    private final AuthenticationInterceptor authenticationInterceptor;
    private final CorsProperties corsProperties;

    @Autowired
    public WebConfigurer(ResponseResultInterceptor interceptor,
                         AuthenticationInterceptor authenticationInterceptor,
                         CorsProperties corsProperties) {
        this.interceptor = interceptor;
        this.authenticationInterceptor = authenticationInterceptor;
        this.corsProperties = corsProperties;
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
        registry.addInterceptor(authenticationInterceptor).addPathPatterns("/**");
        // 注册注解拦截器，并排除不需要注解鉴权的接口地址 (与登录拦截器无关)
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
    }

    /**
     * 配置静态资源，比如html，js，css，等等
     *
     * @param registry registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
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
                .allowedOriginPatterns(corsProperties.getAllowedOriginPatterns().toArray(String[]::new))
                .allowedMethods(corsProperties.getAllowedMethods().toArray(String[]::new))
                .allowedHeaders(corsProperties.getAllowedHeaders().toArray(String[]::new))
                .exposedHeaders("Trace-Id")
                .maxAge(corsProperties.getMaxAge())
                .allowCredentials(corsProperties.isAllowCredentials());
    }
}
