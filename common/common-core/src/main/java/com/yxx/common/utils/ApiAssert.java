package com.yxx.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.yxx.common.exceptions.ApiException;
import com.yxx.common.enums.ApiCode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * API断言
 *
 * @author yxx
 * @since 2022/4/13 14:37
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiAssert {

    /**
     * 两个对象必须相等
     *
     * @param apiCode ApiCode
     * @param obj1    对象1
     * @param obj2    对象2
     */
    public static void equals(ApiCode apiCode, Object obj1, Object obj2) {
        equals(apiCode, obj1, obj2, null);
    }

    /**
     * 条件必须为true
     *
     * @param apiCode   ApiCode
     * @param condition 判断条件
     */
    public static void isTrue(ApiCode apiCode, boolean condition) {
        isTrue(apiCode, condition, null);
    }

    /**
     * 条件必须为false
     *
     * @param apiCode   ApiCode
     * @param condition 判断条件
     */
    public static void isFalse(ApiCode apiCode, boolean condition) {
        isFalse(apiCode, condition, null);
    }

    /**
     * 对象必须为null
     *
     * @param apiCode ApiCode
     * @param obj     校验对象
     */
    public static void isNull(ApiCode apiCode, Object obj) {
        isNull(apiCode, obj, null);
    }

    /**
     * 对象不能为null
     *
     * @param apiCode ApiCode
     * @param obj     校验对象
     */
    public static void isNotNull(ApiCode apiCode, Object obj) {
        isNotNull(apiCode, obj, null);
    }

    /**
     * 数组不能为空
     *
     * @param apiCode ApiCode
     * @param array   数组
     */
    public static void notEmpty(ApiCode apiCode, Object[] array) {
        notEmpty(apiCode, array, null);
    }

    /**
     * 数组不能包含空元素
     *
     * @param apiCode ApiCode
     * @param array   数组
     */
    public static void noNullElements(ApiCode apiCode, Object[] array) {
        noNullElements(apiCode, array, null);
    }

    /**
     * 集合不能为空
     *
     * @param apiCode    ApiCode
     * @param collection 集合对象
     */
    public static void notEmpty(ApiCode apiCode, Collection<?> collection) {
        notEmpty(apiCode, collection, null);
    }

    /**
     * 集合必须为空
     *
     * @param apiCode    ApiCode
     * @param collection 集合对象
     */
    public static void isEmpty(ApiCode apiCode, Collection<?> collection) {
        isEmpty(apiCode, collection, null);
    }

    /**
     * Map不能为空
     *
     * @param apiCode ApiCode
     * @param map     MAP对象
     */
    public static void notEmpty(ApiCode apiCode, Map<?, ?> map) {
        notEmpty(apiCode, map, null);
    }

    /**
     * Map必须为空
     *
     * @param apiCode ApiCode
     * @param map     Map对象
     */
    public static void isEmpty(ApiCode apiCode, Map<?, ?> map) {
        isEmpty(apiCode, map, null);
    }

    /**
     * 两个对象必须相等
     *
     * @param apiCode  ApiCode
     * @param obj1     对象1
     * @param obj2     对象2
     * @param errorMsg 错误描述
     */
    public static void equals(ApiCode apiCode, Object obj1, Object obj2, String errorMsg) {
        if (!Objects.equals(obj1, obj2)) {
            failure(apiCode, errorMsg);
        }
    }

    /**
     * 条件必须为true
     *
     * @param apiCode   ApiCode
     * @param condition 判断条件
     * @param errorMsg  错误描述
     */
    public static void isTrue(ApiCode apiCode, boolean condition, String errorMsg) {
        if (!condition) {
            failure(apiCode, errorMsg);
        }
    }

    /**
     * 条件必须为false
     *
     * @param apiCode   ApiCode
     * @param condition 判断条件
     * @param errorMsg  错误描述
     */
    public static void isFalse(ApiCode apiCode, boolean condition, String errorMsg) {
        if (condition) {
            failure(apiCode, errorMsg);
        }
    }

    /**
     * 对象必须为null
     *
     * @param apiCode  ApiCode
     * @param obj      校验对象
     * @param errorMsg 错误描述
     */
    public static void isNull(ApiCode apiCode, Object obj, String errorMsg) {
        if (ObjectUtil.isNotNull(obj)) {
            failure(apiCode, errorMsg);
        }
    }

    /**
     * 对象不能为null
     *
     * @param apiCode  ApiCode
     * @param obj      校验对象
     * @param errorMsg 错误描述
     */
    public static void isNotNull(ApiCode apiCode, Object obj, String errorMsg) {
        if (ObjectUtil.isNull(obj)) {
            failure(apiCode, errorMsg);
        }
    }

    /**
     * 数组不能为空
     *
     * @param apiCode  ApiCode
     * @param array    数组
     * @param errorMsg 错误描述
     */
    public static void notEmpty(ApiCode apiCode, Object[] array, String errorMsg) {
        if (ObjectUtil.isEmpty(array)) {
            failure(apiCode, errorMsg);
        }
    }

    /**
     * 数组不能包含空元素
     *
     * @param apiCode  ApiCode
     * @param array    数组
     * @param errorMsg 错误描述
     */
    public static void noNullElements(ApiCode apiCode, Object[] array, String errorMsg) {
        if (array != null) {
            for (Object element : array) {
                if (element == null) {
                    failure(apiCode, errorMsg);
                }
            }
        }
    }

    /**
     * 集合不能为空
     *
     * @param apiCode    ApiCode
     * @param collection 集合对象
     * @param errorMsg   错误描述
     */
    public static void notEmpty(ApiCode apiCode, Collection<?> collection, String errorMsg) {
        if (CollUtil.isEmpty(collection)) {
            failure(apiCode, errorMsg);
        }
    }

    /**
     * 集合必须为空
     *
     * @param apiCode    ApiCode
     * @param collection 集合对象
     * @param errorMsg   错误描述
     */
    public static void isEmpty(ApiCode apiCode, Collection<?> collection, String errorMsg) {
        if (CollUtil.isNotEmpty(collection)) {
            failure(apiCode, errorMsg);
        }
    }

    /**
     * Map不能为空
     *
     * @param apiCode  ApiCode
     * @param map      MAP对象
     * @param errorMsg 错误描述
     */
    public static void notEmpty(ApiCode apiCode, Map<?, ?> map, String errorMsg) {
        if (MapUtil.isEmpty(map)) {
            failure(apiCode, errorMsg);
        }
    }

    /**
     * Map必须为空
     *
     * @param apiCode  ApiCode
     * @param map      Map对象
     * @param errorMsg 错误描述
     */
    public static void isEmpty(ApiCode apiCode, Map<?, ?> map, String errorMsg) {
        if (MapUtil.isNotEmpty(map)) {
            failure(apiCode, errorMsg);
        }
    }


    /**
     * 失败结果
     *
     * @param apiCode  ApiCode
     * @param errorMsg 错误描述 如果有值则覆盖原错误提示
     */
    public static void failure(ApiCode apiCode, String errorMsg) {
        if (CharSequenceUtil.isBlank(errorMsg)) {
            throw new ApiException(apiCode);
        }
        throw new ApiException(apiCode.getCode(), errorMsg);
    }
}
