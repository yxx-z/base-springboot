package com.yxx.framework.config.async;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 应用异步任务线程池配置。
 *
 * <p>禁止直接使用 {@code CompletableFuture} 的公共线程池。独立线程池能够限制资源占用、
 * 定义拒绝策略，并在应用关闭时等待正在执行的任务结束。</p>
 */
@Configuration
@EnableAsync
public class AsyncExecutorConfig {

    /**
     * 创建应用级异步执行器。
     *
     * @return 有界异步执行器
     */
    @Bean("applicationTaskExecutor")
    public Executor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("base-async-");
        executor.setTaskDecorator(mdcTaskDecorator());
        // 队列满时由提交线程执行，既形成自然背压，也避免静默丢失安全通知任务。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * 将提交线程的 MDC 快照传递给异步线程，并在任务结束后恢复线程原有上下文。
     *
     * @return MDC 任务装饰器
     */
    private TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> callerContext = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> previousContext = MDC.getCopyOfContextMap();
                try {
                    if (callerContext == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(callerContext);
                    }
                    runnable.run();
                } finally {
                    if (previousContext == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(previousContext);
                    }
                }
            };
        };
    }
}
