package com.yxx.security.context;

import java.util.Set;

/**
 * 会话失效重试耗尽事件。
 *
 * <p>公共安全模块不强制所有应用引入特定监控产品。需要 Prometheus、告警平台或其他
 * 监控能力的项目，可以监听本事件并转换为对应指标或告警。</p>
 *
 * @param realm       安全域，值为 {@code user} 或 {@code admin}
 * @param subjectId   数据库内部主体标识
 * @param reasons     本次事务触发会话失效的原因集合
 * @param attemptCount 包含首次立即执行在内的总尝试次数
 * @param errorType   最后一次异常类型
 * @param errorMessage 最后一次异常信息
 */
public record SessionInvalidationExhaustedEvent(
        String realm,
        Long subjectId,
        Set<SessionInvalidationReason> reasons,
        int attemptCount,
        String errorType,
        String errorMessage) {
}
