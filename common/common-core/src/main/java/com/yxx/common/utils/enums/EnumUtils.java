package com.yxx.common.utils.enums;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class EnumUtils {

    /**
     * 判断值是否在枚举类的code中
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
}