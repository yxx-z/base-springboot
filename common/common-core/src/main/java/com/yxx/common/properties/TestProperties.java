package com.yxx.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author yxx
 * @since 2022-11-12 02:11
 */
@Component
@Data
@ConfigurationProperties(prefix = "test")
public class TestProperties {
    private Integer age;
}
