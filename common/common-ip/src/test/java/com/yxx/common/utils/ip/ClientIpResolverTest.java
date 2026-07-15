package com.yxx.common.utils.ip;

import com.yxx.common.properties.IpProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 客户端 IP 可信代理解析测试。 */
class ClientIpResolverTest {

    @Test
    void shouldIgnoreForwardedHeaderFromUntrustedClient() {
        IpProperties properties = new IpProperties();
        ClientIpResolver resolver = new ClientIpResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.20");

        assertEquals("203.0.113.10", resolver.resolve(request));
    }

    @Test
    void shouldWalkForwardedChainFromTrustedProxySide() {
        IpProperties properties = new IpProperties();
        properties.getTrustedProxies().add("10.0.0.10");
        properties.getTrustedProxies().add("10.0.0.9");
        ClientIpResolver resolver = new ClientIpResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.10");
        request.addHeader("X-Forwarded-For", "192.0.2.66, 198.51.100.20, 10.0.0.9");

        assertEquals("198.51.100.20", resolver.resolve(request));
    }
}
