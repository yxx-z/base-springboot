package com.yxx.common.utils;

import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;

/** 业务条件断言工具。 */
public final class ApiAssert {

    private ApiAssert() {
    }

    /**
     * 要求业务条件成立，否则抛出对应业务异常。
     *
     * @param apiCode 条件不成立时使用的业务错误码
     * @param condition 必须成立的条件
     */
    public static void isTrue(ApiCode apiCode, boolean condition) {
        if (!condition) {
            throw new ApiException(apiCode);
        }
    }
}
