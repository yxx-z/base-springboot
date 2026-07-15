package com.yxx.common.utils.enums;

import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class EnumUtils {

    /**
     * 判断值是否在枚举类的code中
     *
     * @param clzz 枚举类
     * @param code 值
     * @return true-在；false-不在
     */
    public static boolean isInclude(Class<?> clzz, String code) {
        boolean include = false;
        try {
            if (clzz.isEnum()) {
                Object[] enumConstants = clzz.getEnumConstants();
                Method getCode = clzz.getMethod("getCode");
                for (Object enumConstant : enumConstants) {
                    if (getCode.invoke(enumConstant).equals(code)) {
                        include = true;
                        break;
                    }
                }
            }
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            log.error(String.valueOf(e));
        }
        return include;
    }

    /**
     * 根据枚举类code获取message
     *
     * @param enumClass 枚举类
     * @param code      枚举类code
     * @return {@link String }
     * @author yxx
     */
    public static String getMessageByCode(Class<?> enumClass, String code) {
        if (enumClass.isEnum()) {
            Object[] enumConstants = enumClass.getEnumConstants();
            try {
                Field codeField = enumClass.getDeclaredField("code");
                Field messageField = enumClass.getDeclaredField("message");
                codeField.setAccessible(true);
                messageField.setAccessible(true);

                for (Object enumConstant : enumConstants) {
                    String enumCode = (String) codeField.get(enumConstant);
                    String enumMessage = (String) messageField.get(enumConstant);

                    if (code.equals(enumCode)) {
                        return enumMessage;
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                // 处理异常
                e.printStackTrace();
                throw new ApiException(ApiCode.ENUM_ERROR);
            }
        }
        return "未知错误";
    }
}
