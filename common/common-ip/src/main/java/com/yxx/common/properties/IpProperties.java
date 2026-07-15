package com.yxx.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * 可信反向代理的直接连接 IP 白名单。
     *
     * <p>默认列表为空，此时忽略所有转发请求头。部署在 Nginx、Ingress 等网关之后时，
     * 应显式配置实际代理节点 IP，不能配置为任意来源。</p>
     */
    private List<String> trustedProxies = new ArrayList<>();
}
