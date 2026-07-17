package com.yxx.common.http.interceptor;

import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.interceptor.ForestInterceptor;
import org.slf4j.MDC;

/**
 * Forest 出站请求 TraceId 透传拦截器。
 *
 * <p>只透传当前线程 MDC 中已经建立的 TraceId，不在此处生成新的链路标识。入口请求、消息消费或
 * 调度任务应在各自边界建立并清理 MDC；公共 HTTP 客户端如果自行写入 MDC，将无法可靠判断何时清理，
 * 容易在线程复用时造成链路串号。</p>
 */
public class ForestTraceIdInterceptor implements ForestInterceptor {

    private static final String MDC_TRACE_ID_KEY = "Trace-Id";

    private final String headerName;

    public ForestTraceIdInterceptor(String headerName) {
        this.headerName = headerName;
    }

    /**
     * 将当前链路标识写入出站请求头。
     *
     * @param request Forest 请求对象
     * @return 始终返回 {@code true}，不改变请求执行流程
     */
    @Override
    public boolean beforeExecute(ForestRequest request) {
        String traceId = MDC.get(MDC_TRACE_ID_KEY);
        if (traceId != null && !traceId.isBlank()) {
            // addHeader 在 Forest 中采用覆盖语义，确保出站请求使用当前可信调用链的 TraceId。
            request.addHeader(headerName, traceId);
        }
        return true;
    }
}
