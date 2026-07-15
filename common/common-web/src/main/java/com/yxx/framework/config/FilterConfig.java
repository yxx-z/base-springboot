package com.yxx.framework.config;

import com.yxx.framework.filter.RepeatableFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 *
 * @author yxx
 * @since 2022/4/13 17:30
 */
@Configuration
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class FilterConfig {
    @Bean
    public FilterRegistrationBean<RepeatableFilter> requestContextFilterRegistration() {
        FilterRegistrationBean<RepeatableFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RepeatableFilter());
        registration.addUrlPatterns("/*");
        registration.setName("repeatableFilter");
        // TraceId 必须在鉴权、异常处理和业务日志之前完成初始化。
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

}
