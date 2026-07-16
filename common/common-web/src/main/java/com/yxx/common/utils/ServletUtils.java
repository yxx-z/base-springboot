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
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
        // 该工具只用于 Web 请求链；异步任务应在请求线程内提前提取所需信息。
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
            // Filter 已缓存被业务读取过的正文，此处读取副本不会再次消费输入流。
            ContentCachingRequestWrapper cachingRequest = (ContentCachingRequestWrapper) request;
            byte[] content = cachingRequest.getContentAsByteArray();
            cachingRequest.getCharacterEncoding();
            Charset charset = Charset.forName(cachingRequest.getCharacterEncoding());
            params = new String(content, charset);
        } else {
            // 表单和查询参数转换为 JSON，保留同名参数的数组结构。
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
            // 只有会携带正文且已被缓存包装的 JSON 请求才允许读取审计参数。
            return StringUtils.startsWithIgnoreCase(contentType, MediaType.APPLICATION_JSON_VALUE)
                    && request instanceof ContentCachingRequestWrapper
                    && Arrays.asList(HttpMethod.POST.name(), HttpMethod.PUT.name(), HttpMethod.PATCH.name())
                    .contains(request.getMethod());
        }
        return false;
    }
}
