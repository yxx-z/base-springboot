package com.yxx.security.context;

import com.yxx.security.constant.SecurityRealm;
import com.yxx.security.properties.SlidingSessionProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** 会话滑动续期配置和安全域路由测试。 */
class SlidingSessionRenewalServiceTest {

    @Test
    void shouldRenewUserSessionToConfiguredSlidingTimeout() {
        SlidingSessionProperties properties = new SlidingSessionProperties();
        properties.setTimeout(Duration.ofDays(7));
        SessionTimeoutRenewer renewer = mock(SessionTimeoutRenewer.class);
        SlidingSessionRenewalService service = new SlidingSessionRenewalService(properties, renewer);

        service.renewCurrentSession(SecurityRealm.USER);

        verify(renewer).renewUser(Duration.ofDays(7).toSeconds());
    }

    @Test
    void shouldRejectUnknownSecurityRealm() {
        SlidingSessionRenewalService service =
                new SlidingSessionRenewalService(
                        new SlidingSessionProperties(), mock(SessionTimeoutRenewer.class));

        assertThrows(IllegalStateException.class,
                () -> service.renewCurrentSession("unknown"));
    }
}
