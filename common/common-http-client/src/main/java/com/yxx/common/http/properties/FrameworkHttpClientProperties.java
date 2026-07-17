package com.yxx.common.http.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 基础框架 HTTP 客户端扩展配置。
 *
 * <p>连接池、超时、后端实现和重试次数继续使用 Forest 原生 {@code forest.*} 配置；本配置只管理
 * 基础框架额外提供的 TraceId 透传和日志安全策略，避免复制 Forest 的完整配置模型。</p>
 */
@ConfigurationProperties(prefix = "framework.http-client")
public class FrameworkHttpClientProperties {

    /** 是否将当前 MDC 中的 TraceId 透传给下游，默认开启。 */
    private boolean traceIdPropagationEnabled = true;

    /** 下游 TraceId 请求头名称。当前链路标识固定从 MDC 的 {@code Trace-Id} 中读取。 */
    private String traceIdHeaderName = "Trace-Id";

    /** Forest 日志安全策略。 */
    private final Logging logging = new Logging();

    public boolean isTraceIdPropagationEnabled() {
        return traceIdPropagationEnabled;
    }

    public void setTraceIdPropagationEnabled(boolean traceIdPropagationEnabled) {
        this.traceIdPropagationEnabled = traceIdPropagationEnabled;
    }

    public String getTraceIdHeaderName() {
        return traceIdHeaderName;
    }

    public void setTraceIdHeaderName(String traceIdHeaderName) {
        if (traceIdHeaderName == null || traceIdHeaderName.isBlank()) {
            throw new IllegalArgumentException("framework.http-client.trace-id-header-name 不能为空");
        }
        this.traceIdHeaderName = traceIdHeaderName.trim();
    }

    public Logging getLogging() {
        return logging;
    }

    /**
     * Forest 日志开关。
     *
     * <p>所有选项默认关闭。即使只开启请求概要，也要避免把 Token、签名、验证码或其他密钥放进 URL
     * 查询参数，因为 URL 本身属于请求概要的一部分。</p>
     */
    public static class Logging {

        private boolean enabled;
        private boolean request;
        private boolean requestHeaders;
        private boolean requestBody;
        private boolean responseStatus;
        private boolean responseHeaders;
        private boolean responseContent;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isRequest() {
            return request;
        }

        public void setRequest(boolean request) {
            this.request = request;
        }

        public boolean isRequestHeaders() {
            return requestHeaders;
        }

        public void setRequestHeaders(boolean requestHeaders) {
            this.requestHeaders = requestHeaders;
        }

        public boolean isRequestBody() {
            return requestBody;
        }

        public void setRequestBody(boolean requestBody) {
            this.requestBody = requestBody;
        }

        public boolean isResponseStatus() {
            return responseStatus;
        }

        public void setResponseStatus(boolean responseStatus) {
            this.responseStatus = responseStatus;
        }

        public boolean isResponseHeaders() {
            return responseHeaders;
        }

        public void setResponseHeaders(boolean responseHeaders) {
            this.responseHeaders = responseHeaders;
        }

        public boolean isResponseContent() {
            return responseContent;
        }

        public void setResponseContent(boolean responseContent) {
            this.responseContent = responseContent;
        }
    }
}
