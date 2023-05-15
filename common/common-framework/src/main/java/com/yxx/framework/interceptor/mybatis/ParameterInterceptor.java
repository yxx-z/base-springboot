package com.yxx.framework.interceptor.mybatis;

import com.yxx.common.annotation.mybaits.EncryptedField;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.common.utils.encryptor.IEncryptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.util.Objects;

@Intercepts({
        @Signature(type = ParameterHandler.class, method = "setParameters", args = PreparedStatement.class)
})
@Slf4j
public class ParameterInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!(invocation.getTarget() instanceof ParameterHandler)) {
            return invocation.proceed();
        }
        ParameterHandler parameterHandler = (ParameterHandler) invocation.getTarget();
        Object parameterObject = parameterHandler.getParameterObject();
        if (Objects.isNull(parameterObject)) {
            return invocation.proceed();
        }
        Class<?> parameterObjectClass = parameterObject.getClass();
        EncryptedField encryptDecryptClass = parameterObjectClass.getAnnotation(EncryptedField.class);
        if (Objects.nonNull(encryptDecryptClass)) {
            //取出当前当前类所有字段，传入加密方法
            getDeclaredFields(parameterObjectClass);
        } else {
            // 取出该类所有字段
            Field[] fields = parameterObjectClass.getDeclaredFields();
            // 遍历字段
            ep(fields, parameterObject);
        }
        return invocation.proceed();
    }

    static void getDeclaredFields(Class<?> parameterObjectClass) {
        try {
            Field[] declaredFields = parameterObjectClass.getDeclaredFields();
            EncryptedField annotation = parameterObjectClass.getAnnotation(EncryptedField.class);
            Class<? extends IEncryptor> encryptor = annotation.encryptor();
            String key = annotation.key();
            IEncryptor iEncryptor = encryptor.newInstance();
            for (Field declaredField : declaredFields) {
                iEncryptor.encrypt(declaredField, key);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ApiException(ApiCode.SYSTEM_ERROR);
        }
    }

    static void ep(Field[] fields, Object parameterObject) throws Exception {
        for (Field field : fields) {
            field.setAccessible(true);
            // 如果有字段使用 EncryptedField 注解，则对该字段进行加密
            if (field.isAnnotationPresent(EncryptedField.class)) {
                // 获取注解上传入的加密类
                EncryptedField annotation = field.getAnnotation(EncryptedField.class);
                String key = annotation.key();
                Class<? extends IEncryptor> encryptor = annotation.encryptor();
                IEncryptor iEncryptor = encryptor.newInstance();
                Object obj = field.get(parameterObject);
                // 只加密String类型
                if (obj instanceof String) {
                    String encrypt = iEncryptor.encrypt(obj, key);
                    log.info("encrypt:{}", encrypt);
                    field.set(parameterObject, encrypt);
                }
            }
        }
    }

    @Override
    public Object plugin(Object o) {
        return Plugin.wrap(o, this);
    }

}