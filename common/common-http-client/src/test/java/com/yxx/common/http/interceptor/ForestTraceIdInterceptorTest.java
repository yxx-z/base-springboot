package com.yxx.common.http.interceptor;

import com.dtflys.forest.http.ForestRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ForestTraceIdInterceptorTest {

    private static final String TRACE_ID_HEADER = "Trace-Id";

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldPropagateCurrentTraceId() {
        ForestRequest<?> request = mock(ForestRequest.class);
        MDC.put(TRACE_ID_HEADER, "trace_http_client_1234");
        ForestTraceIdInterceptor interceptor = new ForestTraceIdInterceptor("X-Trace-Id");

        boolean proceed = interceptor.beforeExecute(request);

        assertThat(proceed).isTrue();
        verify(request).addHeader("X-Trace-Id", "trace_http_client_1234");
    }

    @Test
    void shouldNotAddHeaderWhenTraceIdIsAbsent() {
        ForestRequest<?> request = mock(ForestRequest.class);
        ForestTraceIdInterceptor interceptor = new ForestTraceIdInterceptor(TRACE_ID_HEADER);

        boolean proceed = interceptor.beforeExecute(request);

        assertThat(proceed).isTrue();
        verifyNoInteractions(request);
    }
}
