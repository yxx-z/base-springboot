package com.yxx.rbac.spi;

/**
 * RBAC 主体存在性校验扩展点。
 *
 * <p>公共 RBAC 模块不依赖 user、admin_user 等具体领域表。每个应用按自己能够管理的主体
 * 提供适配器；例如 business 只提供业务用户适配器，admin 同时提供管理员和业务用户适配器。</p>
 */
public interface RbacSubjectValidator {

    /** 当前适配器是否负责给定主体类型。 */
    boolean supports(String subjectType);

    /** 对应领域表中是否存在该主体。 */
    boolean exists(Long subjectId);
}
