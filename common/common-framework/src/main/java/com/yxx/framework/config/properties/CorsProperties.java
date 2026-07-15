package com.yxx.framework.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 跨域访问配置。
 *
 * <p>默认仅允许本机前端开发地址访问，生产环境必须通过配置文件显式声明可信来源。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "framework.web.cors")
public class CorsProperties {

    /** 允许访问接口的来源模式。 */
    private List<String> allowedOriginPatterns = new ArrayList<>(
            List.of("http://localhost:*", "http://127.0.0.1:*"));

    /** 允许的 HTTP 方法。 */
    private List<String> allowedMethods = new ArrayList<>(
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

    /** 允许客户端发送的请求头。 */
    private List<String> allowedHeaders = new ArrayList<>(
            List.of("Authorization", "Content-Type", "Trace-Id"));

    /** 是否允许浏览器携带 Cookie 等凭据。 */
    private boolean allowCredentials = true;

    /** 预检结果缓存时间，单位为秒。 */
    private long maxAge = 3600L;
}
