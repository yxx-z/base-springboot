package com.yxx.common.http.config;

import com.dtflys.forest.config.ForestConfiguration;
import com.yxx.common.http.interceptor.ForestTraceIdInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class ForestConfigurationCustomizerTest {

    @Test
    void shouldApplySafeLoggingDefaultsAndTraceIdInterceptor() {
        ForestConfiguration forestConfiguration = ForestConfiguration.configuration("safeDefaultsTest");
        ForestConfigurationCustomizer customizer = customizer(new MockEnvironment());

        customizer.postProcessAfterInitialization(forestConfiguration, "forestConfiguration");

        assertThat(forestConfiguration.isLogEnabled()).isFalse();
        assertThat(forestConfiguration.isLogRequest()).isFalse();
        assertThat(forestConfiguration.isLogRequestHeaders()).isFalse();
        assertThat(forestConfiguration.isLogRequestBody()).isFalse();
        assertThat(forestConfiguration.isLogResponseStatus()).isFalse();
        assertThat(forestConfiguration.isLogResponseHeaders()).isFalse();
        assertThat(forestConfiguration.isLogResponseContent()).isFalse();
        assertThat(forestConfiguration.getInterceptors()).containsExactly(ForestTraceIdInterceptor.class);
    }

    @Test
    void shouldAllowExplicitLoggingOptionsAndDisableTracePropagation() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("framework.http-client.trace-id-propagation-enabled", "false")
                .withProperty("framework.http-client.logging.enabled", "true")
                .withProperty("framework.http-client.logging.request", "true")
                .withProperty("framework.http-client.logging.response-status", "true");
        ForestConfiguration forestConfiguration = ForestConfiguration.configuration("explicitOptionsTest");

        customizer(environment).postProcessAfterInitialization(forestConfiguration, "forestConfiguration");

        assertThat(forestConfiguration.isLogEnabled()).isTrue();
        assertThat(forestConfiguration.isLogRequest()).isTrue();
        assertThat(forestConfiguration.isLogResponseStatus()).isTrue();
        assertThat(forestConfiguration.isLogRequestHeaders()).isFalse();
        assertThat(forestConfiguration.getInterceptors()).isNullOrEmpty();
    }

    @Test
    void shouldNotRegisterTraceIdInterceptorTwice() {
        ForestConfiguration forestConfiguration = ForestConfiguration.configuration("idempotentTest");
        ForestConfigurationCustomizer customizer = customizer(new MockEnvironment());

        customizer.postProcessAfterInitialization(forestConfiguration, "forestConfiguration");
        customizer.postProcessAfterInitialization(forestConfiguration, "forestConfiguration");

        assertThat(forestConfiguration.getInterceptors())
                .containsExactly(ForestTraceIdInterceptor.class);
    }

    private ForestConfigurationCustomizer customizer(MockEnvironment environment) {
        ForestConfigurationCustomizer customizer = new ForestConfigurationCustomizer();
        customizer.setEnvironment(environment);
        return customizer;
    }
}
