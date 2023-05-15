package com.yxx.common.annotation.mybaits;

import com.yxx.common.utils.encryptor.DESUtil;
import com.yxx.common.utils.encryptor.IEncryptor;

import java.lang.annotation.*;

/**
 * description:
 * 自定义注解，用来加在类字段上进行mybatis插入更新加密，查询解密
 * 默认key为 eRiw3nvi2lg （key必须大于8位数）
 * 默认加密类为 DESUtil.class
 * 默认查询解密
 *
 * @author yxx
 * @since 2022/11/25
 */
@Documented
@Inherited
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EncryptedField {

    //加密key
    String key() default "eRiw3nvi2lg";

    // 加密类
    Class<? extends IEncryptor> encryptor() default DESUtil.class;

    // 是否解密
    boolean decode() default true;
}
