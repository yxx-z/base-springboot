package com.yxx.security.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 登录会话滑动过期配置。
 *
 * <p>滑动过期与 Sa-Token 的 {@code active-timeout} 不同：每次认证成功后会续签 Token、
 * Token-Session 和 Account-Session 的 Redis TTL。只要主体在配置周期内持续访问，会话就
 * 可以持续有效；连续超过该周期没有访问时，Redis 自动清理会话数据。</p>
 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "security.session.sliding-expiration")
public class SlidingSessionProperties {

    /** 是否启用服务端会话滑动过期。 */
    private boolean enabled = true;

    /** 距离最后一次有效访问多久后会话失效，默认七天。 */
    @NotNull(message = "会话滑动过期时间不能为空")
    private Duration timeout = Duration.ofDays(7);

    /** 启用滑动过期时必须配置正数时长，避免会话被立即清理或变成永久有效。 */
    @AssertTrue(message = "会话滑动过期时间必须大于0")
    public boolean isTimeoutValid() {
        return !enabled || (timeout != null && !timeout.isZero() && !timeout.isNegative());
    }
}
