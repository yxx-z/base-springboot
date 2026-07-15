package com.yxx.security.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 密码登录防暴力破解配置。 */
@Data
@Component
@ConfigurationProperties(prefix = "security.login-protection")
public class LoginProtectionProperties {

    /** 单个账号在统计窗口内允许的最大失败次数。 */
    private int accountMaxFailures = 5;

    /** 单个客户端 IP 在统计窗口内允许的最大失败次数。 */
    private int ipMaxFailures = 20;

    /** 登录失败统计窗口。 */
    private Duration window = Duration.ofMinutes(15);
}
