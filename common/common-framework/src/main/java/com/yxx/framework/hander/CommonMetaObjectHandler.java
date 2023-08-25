package com.yxx.framework.hander;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.yxx.common.utils.auth.LoginUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * @author yxx
 * @since 2022/4/13 11:23
 */
@Slf4j
public class CommonMetaObjectHandler implements MetaObjectHandler {

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
            return LoginUtils.getUserId();
        } catch (Exception ignore) {
            log.error("生成uid错误");
        }
        return 1L;
    }
}
