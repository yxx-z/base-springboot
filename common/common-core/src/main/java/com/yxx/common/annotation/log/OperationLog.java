package com.yxx.common.annotation.log;

import java.lang.annotation.*;

/**
 * 自定义操作日志注解
 *
 * @author yxx
 * @since 2022/7/26 11:19
 */
@Documented
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /**
     * 操作模块 为空的话取@Api的tags值
     */
    String module() default "";

    /**
     * 日志标题 为空的话取@ApiOperation的Value值
     */
    String title() default "";

}
