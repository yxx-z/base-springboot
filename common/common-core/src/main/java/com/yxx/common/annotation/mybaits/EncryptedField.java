package com.yxx.common.annotation.mybaits;

import com.yxx.common.utils.encryptor.AesGcmEncryptor;
import com.yxx.common.utils.encryptor.IEncryptor;

import java.lang.annotation.*;

/**
 * description:
 * 自定义注解，用来加在类字段上进行mybatis插入更新加密，查询解密
 * 默认加密实现为 AES-GCM。生产项目应从外部配置提供密钥，禁止长期使用注解默认值。
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

    /**
     * 字段加密密钥。实际项目应通过自定义处理器从安全配置中获取，不应依赖默认值。
     */
    String key() default "change-this-key";

    // 加密类
    Class<? extends IEncryptor> encryptor() default AesGcmEncryptor.class;

    // 是否解密
    boolean decode() default true;
}
