package com.yxx.framework.hander;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.yxx.security.context.CurrentActorProvider;
import com.yxx.security.model.CurrentActor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * @author yxx
 * @since 2022/4/13 11:23
 */
@Slf4j
public class CommonMetaObjectHandler implements MetaObjectHandler {
    /** 未登录任务、初始化脚本等系统行为使用的审计用户编号。 */
    private static final Long SYSTEM_USER_ID = 0L;

    private final CurrentActorProvider currentActorProvider;

    public CommonMetaObjectHandler(CurrentActorProvider currentActorProvider) {
        this.currentActorProvider = currentActorProvider;
    }

    /**
     * 创建人
     */
    private static final String CREATE_UID = "createUid";

    /**
     * 更新人
     */
    private static final String UPDATE_UID = "updateUid";

    /**
     * 创建时间
     */
    private static final String CREATE_TIME = "createTime";

    /**
     * 修改时间
     */
    private static final String UPDATE_TIME = "updateTime";

    @Override
    public void insertFill(MetaObject metaObject) {
        Long uid = currentUid();
        strictInsertFill(metaObject, CREATE_UID, Long.class, uid);
        strictInsertFill(metaObject, UPDATE_UID, Long.class, uid);
        strictInsertFill(metaObject, CREATE_TIME, LocalDateTime.class, LocalDateTime.now());
        strictInsertFill(metaObject, UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, UPDATE_UID, Long.class, currentUid());
        strictUpdateFill(metaObject, UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
    }

    public Long currentUid() {
        try {
            return currentActorProvider.currentActor()
                    .map(CurrentActor::actorId)
                    .orElse(SYSTEM_USER_ID);
        } catch (RuntimeException exception) {
            log.debug("当前线程不存在登录上下文，审计用户使用系统账号", exception);
        }
        return SYSTEM_USER_ID;
    }
}
