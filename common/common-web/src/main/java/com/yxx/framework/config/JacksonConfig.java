package com.yxx.framework.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yxx.common.utils.jackson.JacksonUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

/**
 * 应用 JSON 配置。
 *
 * <p>Spring MVC、Redis 派生 Mapper 与少量兼容性静态工具共享同一套日期和反序列化规则，
 * 避免不同基础设施对同一对象产生不一致的 JSON 语义。</p>
 */
@Configuration
@RequiredArgsConstructor
public class JacksonConfig {

    private final ObjectMapper objectMapper;

    /** 在应用启动阶段统一配置 Spring Boot 管理的 ObjectMapper。 */
    @PostConstruct
    public void configure() {
        JacksonUtil.initObjectMapper(objectMapper);
    }
}
