package com.yxx.security.model;

/**
 * 审计体系使用的最小操作人信息。
 *
 * <p>审计模块只需要知道“谁执行了操作”，不应依赖完整登录主体，更不能依赖具体业务用户
 * 实体。该模型用于隔离认证实现与操作审计。</p>
 *
 * @param actorId    操作人内部稳定标识
 * @param actorType  操作人类型
 * @param actorAccount 事件发生时的登录账号快照
 * @param actorName  操作人显示名称
 */
public record CurrentActor(Long actorId, String actorType, String actorAccount, String actorName) {
}
