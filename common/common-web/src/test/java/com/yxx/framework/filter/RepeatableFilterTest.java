package com.yxx.framework.filter;

import com.yxx.framework.context.AppContext;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 请求 TraceId 生命周期测试。
 */
class RepeatableFilterTest {

    private final RepeatableFilter filter = new RepeatableFilter();

    @Test
    void shouldReuseValidClientTraceIdAndClearContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AppContext.KEY_TRACE_ID, "client_trace_1234");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertEquals("client_trace_1234", AppContext.getTraceId());
            assertEquals("client_trace_1234", MDC.get(AppContext.KEY_TRACE_ID));
        });

        assertEquals("client_trace_1234", response.getHeader(AppContext.KEY_TRACE_ID));
        assertNull(AppContext.getTraceId());
        assertNull(MDC.get(AppContext.KEY_TRACE_ID));
    }

    @Test
    void shouldGenerateTraceIdWhenHeaderIsInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AppContext.KEY_TRACE_ID, "bad id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertNotNull(AppContext.getTraceId()));

        assertNotNull(response.getHeader(AppContext.KEY_TRACE_ID));
        assertNull(AppContext.getTraceId());
    }

    @Test
    void shouldCacheOnlyJsonRequestForAudit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent("{\"name\":\"测试\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) servletRequest;
            wrapper.getInputStream().readAllBytes();
            assertEquals("{\"name\":\"测试\"}",
                    new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8));
        });
    }
}
