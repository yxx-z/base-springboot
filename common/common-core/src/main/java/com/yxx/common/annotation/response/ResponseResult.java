package com.yxx.common.annotation.response;

import java.lang.annotation.*;

/**
 * @author yxx
 * @description: controller加该注解后 可统一返回结果json格式
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface ResponseResult {

}