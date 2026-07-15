package com.yxx.framework.filter;

import cn.hutool.core.text.CharSequenceUtil;
import com.yxx.common.utils.ApplicationUtils;
import com.yxx.common.utils.RepeatedlyRequestWrapper;
import com.yxx.common.utils.SnowflakeConfig;
import com.yxx.framework.context.AppContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * Repeatable过滤器
 *
 * @author yxx
 * @since 2022/4/13 17:29
 */
@Slf4j
public class RepeatableFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        ServletRequest requestWrapper = null;
        if (request instanceof HttpServletRequest httpServletRequest
                && StringUtils.startsWithIgnoreCase(request.getContentType(), MediaType.APPLICATION_JSON_VALUE)) {
            requestWrapper = new RepeatedlyRequestWrapper(httpServletRequest, response);
            String traceId = httpServletRequest.getHeader("Trace-Id");
            if (CharSequenceUtil.isNotBlank(traceId)) {
                log.info(traceId);
            } else {
                SnowflakeConfig snowflake = ApplicationUtils.getBean(SnowflakeConfig.class);
                AppContext.getContext().setTraceId(snowflake.orderNum());
            }
        }
        if (null == requestWrapper) {
            chain.doFilter(request, response);
        } else {
            chain.doFilter(requestWrapper, response);
        }
    }

}
