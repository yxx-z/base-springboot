package com.yxx.common.utils;

import cn.hutool.core.map.MapUtil;
import com.yxx.common.utils.jackson.JacksonUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Map;

/**
 * Spring Application 工具类
 *
 * @author yxx
 * @since 2022/4/13 11:45
 */
public class ServletUtils {

    public static ServletRequestAttributes getRequestAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return (ServletRequestAttributes) attributes;
    }

    /**
     * 获取request
     */
    public static HttpServletRequest getRequest() {
        return getRequestAttributes().getRequest();
    }

    /**
     * 获取请求参数
     *
     * @param request HttpServletRequest
     * @return 请求参数
     */
    public static String getRequestParms(HttpServletRequest request) {
        String params = "";
        if (isJsonRequest(request)) {
            params = new RequestWrapper(request).getBodyString();
        } else {
            Map<String, String[]> parameterMap = request.getParameterMap();
            if (MapUtil.isNotEmpty(parameterMap)) {
                params = JacksonUtil.toJson(parameterMap);
            }
        }
        return params;
    }

    /**
     * 判断本次请求的数据类型是否为json
     *
     * @param request request
     * @return boolean
     */
    public static boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType != null) {
            return StringUtils.startsWithIgnoreCase(contentType, MediaType.APPLICATION_JSON_VALUE)
                    && request instanceof RepeatedlyRequestWrapper
                    && Arrays.asList(HttpMethod.POST.name(), HttpMethod.PUT.name()).contains(request.getMethod());
        }
        return false;
    }
}
