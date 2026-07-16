package com.yxx.framework.filter;

import com.yxx.framework.context.AppContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 请求上下文与审计请求体缓存过滤器。
 *
 * <p>该过滤器负责为每个请求初始化 TraceId，并在请求完成后可靠清理线程上下文。
 * 对 JSON 请求额外增加有限缓存，以支持操作日志在业务读取后获取参数。</p>
 */
public class RepeatableFilter implements Filter {

    /**
     * 审计只缓存前 64 KiB 请求体，避免大 JSON 请求在高并发下造成额外内存压力。
     * 超出部分仍可由业务正常读取，只是不进入审计参数。
     */
    private static final int AUDIT_REQUEST_CACHE_LIMIT = 64 * 1024;

    /**
     * 仅接受长度为 8 至 64 的安全字符，避免客户端通过请求头污染日志内容。
     */
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)
                || !(response instanceof HttpServletResponse httpResponse)) {
            // 非 HTTP 请求保持容器原始行为，不做不安全的强制转换。
            chain.doFilter(request, response);
            return;
        }

        // 同一 TraceId 同时写入业务上下文、日志 MDC 和响应头，贯穿调用与排障链路。
        String traceId = resolveTraceId(httpRequest);
        AppContext.setTraceId(traceId);
        MDC.put(AppContext.KEY_TRACE_ID, traceId);
        httpResponse.setHeader(AppContext.KEY_TRACE_ID, traceId);

        // 只包装需要审计正文的 JSON 请求，文件上传等大请求不进入额外缓存。
        ServletRequest requestToUse = wrapJsonRequestIfNecessary(httpRequest);
        try {
            chain.doFilter(requestToUse, response);
        } finally {
            // Tomcat 工作线程会被重复使用，必须在 finally 中同时清理业务上下文和 MDC。
            MDC.remove(AppContext.KEY_TRACE_ID);
            AppContext.clear();
        }
    }

    /**
     * 解析客户端 TraceId；格式不可信或未传入时生成新的随机标识。
     *
     * @param request HTTP 请求
     * @return 可安全写入日志的 TraceId
     */
    private String resolveTraceId(HttpServletRequest request) {
        String candidate = StringUtils.trim(request.getHeader(AppContext.KEY_TRACE_ID));
        if (candidate != null && TRACE_ID_PATTERN.matcher(candidate).matches()) {
            // 合法上游 TraceId 原样沿用，以支持网关到应用的链路关联。
            return candidate;
        }
        // 不可信值直接丢弃并生成服务端标识，防止换行等内容污染日志。
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * JSON 请求增加有限内容缓存，其他请求保持 Servlet 容器原始行为。
     *
     * @param request  HTTP 请求
     * @return 实际进入过滤器链的请求对象
     */
    private ServletRequest wrapJsonRequestIfNecessary(HttpServletRequest request) {
        if (StringUtils.startsWithIgnoreCase(request.getContentType(), MediaType.APPLICATION_JSON_VALUE)) {
            return new ContentCachingRequestWrapper(request, AUDIT_REQUEST_CACHE_LIMIT);
        }
        return request;
    }
}
