package com.yxx.security.properties;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库提交后会话失效重试配置。
 *
 * <p>首次 Redis 注销始终在事务提交后立即执行；本配置只控制首次失败后的有限异步
 * 重试。重试任务保存在 JVM 内存中，不提供跨进程持久化保证，这是通用基础框架在
 * 可靠性与复杂度之间的明确取舍。</p>
 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "security.session.invalidation")
public class SessionInvalidationProperties {

    /** 是否在首次 Redis 注销失败后执行有限异步重试。 */
    private boolean retryEnabled = true;

    /** 每次重试相对于上一次失败的等待时间，列表长度即最大重试次数。 */
    private List<Duration> retryDelays = new ArrayList<>(
            List.of(Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(30)));

    /** 重试间隔必须全部为正数，避免无间隔循环或非法调度时间。 */
    @AssertTrue(message = "会话失效重试间隔必须全部大于0，且最多配置10次")
    public boolean isRetryDelaysValid() {
        if (!retryEnabled) {
            return true;
        }
        return retryDelays != null
                && !retryDelays.isEmpty()
                && retryDelays.size() <= 10
                && retryDelays.stream().allMatch(delay -> delay != null
                && !delay.isZero() && !delay.isNegative());
    }
}
