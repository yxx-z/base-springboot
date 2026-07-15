package com.yxx.framework.context;

/**
 * 当前请求上下文。
 *
 * <p>上下文只允许在一次同步 HTTP 请求内使用。请求结束后必须调用 {@link #clear()}，
 * 避免容器线程复用时将上一请求的链路信息带入下一请求。</p>
 */
public final class AppContext {

    /** 链路标识对应的请求头及 MDC 键名。 */
    public static final String KEY_TRACE_ID = "Trace-Id";

    private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<>();

    private AppContext() {
    }

    /**
     * 获取当前请求链路标识。
     *
     * @return TraceId；尚未初始化时返回 {@code null}
     */
    public static String getTraceId() {
        return TRACE_ID_HOLDER.get();
    }

    /**
     * 保存当前请求链路标识。
     *
     * @param traceId TraceId
     */
    public static void setTraceId(String traceId) {
        TRACE_ID_HOLDER.set(traceId);
    }

    /**
     * 清理当前线程上下文。
     */
    public static void clear() {
        TRACE_ID_HOLDER.remove();
    }
}
