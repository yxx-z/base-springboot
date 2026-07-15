package com.yxx.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 网站
 *
 * @author yxx
 * @classname WebProperties
 * @since 2023-08-06 02:24
 */
@Component
@Data
@ConfigurationProperties(prefix = "web")
public class MyWebProperties {
    /**
     * 域名
     */
    private String domain;
}
