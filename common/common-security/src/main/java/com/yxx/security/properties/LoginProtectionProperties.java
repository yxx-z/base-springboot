package com.yxx.security.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;

import java.time.Duration;

/** 密码登录防暴力破解配置。 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "security.login-protection")
public class LoginProtectionProperties {

    /** 单个账号在统计窗口内允许的最大失败次数。 */
    @Min(value = 1, message = "账号登录失败阈值必须大于0")
    private int accountMaxFailures = 5;

    /** 单个客户端 IP 在统计窗口内允许的最大失败次数。 */
    @Min(value = 1, message = "IP登录失败阈值必须大于0")
    private int ipMaxFailures = 20;

    /** 登录失败统计窗口。 */
    @NotNull(message = "登录失败统计窗口不能为空")
    private Duration window = Duration.ofMinutes(15);

    @AssertTrue(message = "登录失败统计窗口必须大于0")
    public boolean isWindowValid() {
        return window != null && !window.isZero() && !window.isNegative();
    }
}
