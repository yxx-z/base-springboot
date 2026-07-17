package com.yxx.common.http;

import com.yxx.common.http.testapp.ForestTestApplication;
import com.yxx.common.http.testapp.TraceTestClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ForestTestApplication.class)
class ForestHttpClientIntegrationTest {

    private static final String TRACE_ID_HEADER = "Trace-Id";

    private static MockWebServer mockWebServer;

    @Autowired
    private TraceTestClient traceTestClient;

    @BeforeAll
    static void startServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void stopServer() throws IOException {
        mockWebServer.shutdown();
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldExecuteRequestAndPropagateTraceId() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
        MDC.put(TRACE_ID_HEADER, "trace_forest_integration_1234");

        String baseUrl = mockWebServer.url("/").toString();
        String response = traceTestClient.getTrace(baseUrl.substring(0, baseUrl.length() - 1));
        RecordedRequest request = mockWebServer.takeRequest(3, TimeUnit.SECONDS);

        assertThat(response).isEqualTo("ok");
        assertThat(request).isNotNull();
        assertThat(request.getPath()).isEqualTo("/trace");
        assertThat(request.getHeader(TRACE_ID_HEADER)).isEqualTo("trace_forest_integration_1234");
    }
}
