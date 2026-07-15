package com.yxx.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ip
 *
 * @author yxx
 * @classname IpProperties
 * @since 2023-08-05 23:13
 */
@Component
@Data
@ConfigurationProperties(prefix = "ip")
public class IpProperties {
    /**
     * 是否校验
     */
    private Boolean check;
}
