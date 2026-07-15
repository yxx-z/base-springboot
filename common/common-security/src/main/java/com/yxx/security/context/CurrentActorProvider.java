package com.yxx.security.context;

import com.yxx.security.model.CurrentActor;

import java.util.Optional;

/**
 * 当前操作人提供者。
 *
 * <p>日志、数据审计等基础设施只能依赖该抽象，不允许直接读取 Sa-Token Session 或强转具体
 * 登录用户类型。</p>
 */
public interface CurrentActorProvider {

    /**
     * 获取当前线程对应的操作人。
     *
     * @return 已登录时返回操作人，匿名请求返回空
     */
    Optional<CurrentActor> currentActor();
}
