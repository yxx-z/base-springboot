package com.yxx.framework.interceptor.mybatis;

import com.yxx.common.annotation.mybaits.EncryptedField;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.common.utils.encryptor.IEncryptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.*;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Objects;


@Slf4j
@Intercepts({
        @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})
})
public class ResultSetInterceptor implements Interceptor {


    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        //取出查询的结果
        Object resultObject = invocation.proceed();
        if (Objects.isNull(resultObject)) {
            return null;
        }
        //基于selectList
        if (resultObject instanceof ArrayList) {
            ArrayList<?> resultList = (ArrayList<?>) resultObject;
            if (!CollectionUtils.isEmpty(resultList) && needToDecrypt(resultList.get(0))) {
                for (Object result : resultList) {
                    //逐一解密
                    decrypt(result);
                }
            }
            //基于selectOne
        } else {
            if (needToDecrypt(resultObject)) {
                decrypt(resultObject);
            }
        }
        return resultObject;
    }


    private boolean needToDecrypt(Object object) {
        Class<?> objectClass = object.getClass();
        Field[] declaredFields = objectClass.getDeclaredFields();
        return ep(declaredFields, object);
    }

    static boolean ep(Field[] fields, Object parameterObject) {
        try {
            boolean result = false;
            for (Field field : fields) {
                field.setAccessible(true);
                // 如果有字段使用 EncryptedField 注解,则返回true
                if (field.isAnnotationPresent(EncryptedField.class)) {
                    Object obj = field.get(parameterObject);
                    // 只加密String类型
                    if (obj instanceof String) {
                        result = true;
                    }
                }
            }
            return result;
        } catch (IllegalAccessException e) {
            throw new ApiException(ApiCode.SYSTEM_ERROR);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    void decrypt(Object result) {
        try {
            Class<?> resultClass = result.getClass();
            Field[] declaredFields = resultClass.getDeclaredFields();
            for (Field field : declaredFields) {
                //取出所有被DecryptTransaction注解的字段
                EncryptedField annotation = field.getAnnotation(EncryptedField.class);
                if (!Objects.isNull(annotation)) {
                    boolean decode = annotation.decode();
                    // 设置可访问，否则会发生非法访问异常
                    field.setAccessible(true);
                    Object object = field.get(result);
                    //String的解密
                    if (decode && object instanceof String) {
                        // 获取加密key
                        String key = annotation.key();
                        // 获取加密类
                        Class<? extends IEncryptor> encryptor = annotation.encryptor();
                        // 加密类实例化
                        IEncryptor iEncryptor = encryptor.newInstance();

                        String value = (String) object;
                        log.info("密文:{}", value);
                        //对注解的字段进行逐一解密
                        dispose(result, field, iEncryptor, value, key);
                    }
                }
            }
        } catch (IllegalAccessException | InstantiationException e) {
            throw new ApiException(ApiCode.SYSTEM_ERROR);
        }
    }

    void dispose(Object result, Field field, IEncryptor iEncryptor, String value, String key) {
        String proclaimedInWriting = iEncryptor.decrypt(value, key);
        log.info("明文:{}", proclaimedInWriting);
        try {
            field.set(result, iEncryptor.decrypt(value, key));
        } catch (IllegalAccessException e) {
            throw new ApiException(ApiCode.SYSTEM_ERROR);
        }
    }
}
