package com.yxx.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** 会话失效有限重试使用的独立调度线程池配置。 */
@Configuration
public class SessionInvalidationRetryConfig {

    /**
     * 创建会话失效重试调度器。
     *
     * <p>安全会话清理与邮件、风险通知、普通审计任务隔离，避免其他异步任务积压导致
     * Redis 注销重试无法按时执行。应用关闭时等待短时间完成已开始的任务，但未执行的
     * 内存任务不会持久化到下一次启动。</p>
     *
     * @return 独立的安全会话失效调度器
     */
    @Bean("sessionInvalidationTaskScheduler")
    public TaskScheduler sessionInvalidationTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("session-invalidation-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.initialize();
        return scheduler;
    }
}
