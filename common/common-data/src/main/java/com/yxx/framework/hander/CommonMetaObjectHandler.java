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
        // 同一次插入统一使用当前操作人，创建人与最后更新人初始值保持一致。
        Long uid = currentUid();
        strictInsertFill(metaObject, CREATE_UID, Long.class, uid);
        strictInsertFill(metaObject, UPDATE_UID, Long.class, uid);
        // 创建和更新时间分别填充，允许实体显式提供值时由 strictFill 规则决定是否覆盖。
        strictInsertFill(metaObject, CREATE_TIME, LocalDateTime.class, LocalDateTime.now());
        strictInsertFill(metaObject, UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新操作只改变最后更新人和更新时间，创建审计字段永久保留。
        strictUpdateFill(metaObject, UPDATE_UID, Long.class, currentUid());
        strictUpdateFill(metaObject, UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
    }

    public Long currentUid() {
        try {
            // Web 请求读取登录主体，定时任务、迁移和 bootstrap 等无主体流程使用系统账号。
            return currentActorProvider.currentActor()
                    .map(CurrentActor::actorId)
                    .orElse(SYSTEM_USER_ID);
        } catch (RuntimeException exception) {
            // 审计字段填充不应因缺少请求上下文而中断数据写入。
            log.debug("当前线程不存在登录上下文，审计用户使用系统账号", exception);
        }
        return SYSTEM_USER_ID;
    }
}
