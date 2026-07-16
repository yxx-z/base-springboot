package com.yxx.business.auth;

import com.yxx.business.auth.command.PasswordAuthenticationCommand;
import com.yxx.business.auth.command.UserAuthenticationCommand;
import com.yxx.business.auth.model.AuthenticatedUser;
import com.yxx.business.auth.strategy.UserAuthenticationStrategy;
import com.yxx.business.model.entity.User;
import com.yxx.security.constant.LoginMode;
import com.yxx.security.constant.SecurityRealm;
import com.yxx.security.authorization.AuthorizationProvider;
import com.yxx.security.context.LoginSessionService;
import com.yxx.security.model.AuthorizationSnapshot;
import com.yxx.security.model.LoginPrincipal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 统一认证策略编排测试。
 */
class UserAuthenticationServiceTest {

    @Test
    void shouldBuildUnifiedPrincipalFromAuthenticationStrategy() {
        User user = new User();
        user.setId(10L);
        user.setDisplayName("测试用户");
        UserAuthenticationStrategy strategy = new StubStrategy(
                new AuthenticatedUser(user, "tester", LoginMode.PASSWORD));
        AuthorizationProvider authorizationProvider = mock(AuthorizationProvider.class);
        LoginSessionService loginSessionService = mock(LoginSessionService.class);
        UserLoginRiskService riskService = mock(UserLoginRiskService.class);
        when(authorizationProvider.load(SecurityRealm.USER, 10L)).thenReturn(
                new AuthorizationSnapshot(
                        Set.of("business:member"), Set.of("business:profile:read")));
        when(loginSessionService.loginUser(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("pc"))).thenReturn("token-value");

        UserAuthenticationService service = new UserAuthenticationService(
                List.of(strategy), authorizationProvider, loginSessionService, riskService);
        String token = service.login(new PasswordAuthenticationCommand("tester", "password"), "pc");

        assertEquals("token-value", token);
        ArgumentCaptor<LoginPrincipal> principalCaptor = ArgumentCaptor.forClass(LoginPrincipal.class);
        verify(loginSessionService).loginUser(principalCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("pc"));
        LoginPrincipal principal = principalCaptor.getValue();
        assertEquals(10L, principal.getSubjectId());
        assertEquals(Set.of("business:member"), principal.getRoles());
        assertEquals(Set.of("business:profile:read"), principal.getPermissions());
        verify(riskService).handleSuccessfulLogin(user);
    }

    @Test
    void shouldRejectDuplicateLoginStrategies() {
        UserAuthenticationStrategy first = new StubStrategy(null);
        UserAuthenticationStrategy second = new StubStrategy(null);

        assertThrows(IllegalStateException.class, () -> new UserAuthenticationService(
                List.of(first, second), mock(AuthorizationProvider.class),
                mock(LoginSessionService.class), mock(UserLoginRiskService.class)));
    }

    private record StubStrategy(AuthenticatedUser result) implements UserAuthenticationStrategy {

        @Override
        public String loginMode() {
            return LoginMode.PASSWORD;
        }

        @Override
        public AuthenticatedUser authenticate(UserAuthenticationCommand command) {
            return result;
        }
    }
}
