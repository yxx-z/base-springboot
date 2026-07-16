package com.yxx.business.integration;

import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import cn.dev33.satoken.temp.SaTempUtil;
import com.jayway.jsonpath.JsonPath;
import com.yxx.business.BusinessApplication;
import com.yxx.rbac.model.RbacSubjectType;
import com.yxx.rbac.service.RbacRolePermissionService;
import com.yxx.rbac.service.RbacSubjectRoleService;
import com.yxx.business.service.UserService;
import com.yxx.common.constant.RedisKeyPrefix;
import com.yxx.common.utils.email.MailUtils;
import com.yxx.common.utils.redis.RedissonCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 使用真实 MySQL、Redis 和完整 Spring 上下文验证用户端启动、Mapper、认证及 Sa-Token Session。
 */
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("integration")
@SpringBootTest(classes = BusinessApplication.class, properties = {
        "ip.check=false",
        "mail.from=integration@example.com",
        "mail.from-name=集成测试",
        "mail.register-time=5",
        "mail.register-max=10",
        "mail.register-content={captcha}",
        "mail.ip-unusual-content={ip}",
        "reset-password.base-path=http://localhost/reset",
        "reset-password.max-number=10",
        "reset-password.reset-pwd-time=15",
        "reset-password.reset-pwd-content={url}",
        "web.domain=http://localhost",
        "spring.mail.host=localhost",
        "ali.app-id=test",
        "ali.merchant-private-key=test",
        "ali.charset=UTF-8",
        "ali.alipay-public-key=test",
        "ali.sign-type=RSA2",
        "ali.server-url=https://openapi.alipay.com/gateway.do",
        "ali.format=json",
        "sa-token.timeout=3600",
        "sa-token.active-timeout=1800",
        "sa-token.token-name=Authorization",
        "sa-token.token-prefix=Bearer",
        "sa-token.is-concurrent=false",
        "sa-token.is-share=false",
        "sa-token.token-style=uuid",
        "sa-token.is-log=false",
        "sa-token.is-print=false"
})
@AutoConfigureMockMvc
class BusinessApplicationIntegrationTest {

    private static final Pattern RESET_TOKEN_PATTERN = Pattern.compile("[?&]token=([^<\\\"]+)");

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("business_app_it")
            .withUsername("integration")
            .withPassword("integration");

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.database", () -> 0);
        registry.add("sa-token.alone-redis.host", REDIS::getHost);
        registry.add("sa-token.alone-redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("sa-token.alone-redis.database", () -> 1);
    }

    @MockitoBean
    private AlipayClient alipayClient;

    @MockitoBean
    private MailUtils mailUtils;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedissonCache redissonCache;

    @Autowired
    private RbacSubjectRoleService subjectRoleService;

    @Autowired
    private RbacRolePermissionService rolePermissionService;

    @Autowired
    private UserService userService;

    @BeforeEach
    void cleanBusinessData() {
        clearInvocations(alipayClient, mailUtils);
        jdbcTemplate.update("DELETE FROM operate_log");
        jdbcTemplate.update("DELETE FROM rbac_subject_role WHERE subject_type = 'user'");
        jdbcTemplate.update("DELETE FROM user_identity");
        jdbcTemplate.update("DELETE FROM `user`");
    }

    @Test
    void shouldStartApplicationAndPersistSessionInRedis() throws Exception {
        String loginCode = "integration-" + UUID.randomUUID().toString().substring(0, 8);
        long userId = createPasswordUser(loginCode, "Framework2026", true, true, true);

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginCode":" %s ","password":"Framework2026"}
                                """.formatted(loginCode.toUpperCase())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(response, "$.data.token");

        // Sa-Token 使用独立 Redis 数据库保存登录态，不能只验证 HTTP 响应中的 Token 字符串。
        String sessionKeyCount = REDIS.execInContainer(
                "redis-cli", "-n", "1", "DBSIZE").getStdout().trim();
        assertTrue(Integer.parseInt(sessionKeyCount) > 0, "登录成功后独立 Session Redis 应存在数据");

        mockMvc.perform(get("/user/info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.account").value(loginCode));

        // business 仍持久化审计日志，但不再向业务角色暴露全局日志读取权限。
        assertEquals(0L, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM rbac_permission
                WHERE scope = 'business' AND code = 'business:audit-log:read'
                  AND is_delete = 0
                """, Long.class));
    }

    @Test
    void shouldInvalidateHistoricalSessionWhenUserIsEnabledAgain() throws Exception {
        String account = "reenabled-" + UUID.randomUUID().toString().substring(0, 8);
        long userId = createPasswordUser(account, "Framework2026", true, true, true);
        String historicalToken = loginAndGetToken(account, "Framework2026");

        /*
         * 直接修改状态模拟停用动作已经提交、但 Redis 注销失败后旧 Token 仍残留。
         * 正式启用服务必须再次清理历史会话，禁止旧 Token 随账号启用自动恢复。
         */
        jdbcTemplate.update("UPDATE `user` SET status = 0 WHERE id = ?", userId);
        userService.changeStatus(userId, true);

        assertUserTokenInvalid(historicalToken);
    }

    @Test
    void shouldEnforceIdentityStateAndAtomicLoginRateLimit() throws Exception {
        String disabledIdentity = "disabled-" + UUID.randomUUID().toString().substring(0, 8);
        createPasswordUser(disabledIdentity, "Framework2026", false, true, true);
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(disabledIdentity, "Framework2026")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(2013));

        String unverifiedIdentity = "unverified-" + UUID.randomUUID().toString().substring(0, 8);
        createPasswordUser(unverifiedIdentity, "Framework2026", true, false, true);
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(unverifiedIdentity, "Framework2026")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(2014));

        String protectedAccount = "limited-" + UUID.randomUUID().toString().substring(0, 8);
        createPasswordUser(protectedAccount, "Framework2026", true, true, true);
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(protectedAccount, "WrongPassword")))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(protectedAccount, "WrongPassword")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(2012));
    }

    @Test
    void shouldConsumeRegistrationCaptchaOnlyOnceUnderConcurrentRequests() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "register-" + suffix + "@example.com";
        String account = "register-" + suffix;
        String captcha = "246810";
        redissonCache.putString(RedisKeyPrefix.EMAIL_REGISTER + email, captcha, 300);

        String requestBody = """
                {
                  "loginCode":"%s",
                  "loginName":"并发注册用户",
                  "password":"Framework2026",
                  "linkPhone":"13800138000",
                  "email":"%s",
                  "captcha":"%s"
                }
                """.formatted(account, email, captcha);

        List<MvcResult> results = executeConcurrently(2, () -> mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn());

        long successCount = results.stream()
                .filter(result -> result.getResponse().getStatus() == 200)
                .count();
        assertEquals(1, successCount, "同一个验证码只能有一个并发注册请求成功");
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE email = ?", Integer.class, email));
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM user_identity
                WHERE identity_type = 'password' AND identifier = ?
                """, Integer.class, account));
        assertFalse(redissonCache.exists(RedisKeyPrefix.EMAIL_REGISTER + email));
    }

    @Test
    void shouldAllowRegistrationAgainAfterSoftDeletionAndKeepHistory() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String account = "re-register-" + suffix;
        String email = "re-register-" + suffix + "@example.com";
        String phone = "139" + String.format("%08d", Math.abs(suffix.hashCode()) % 100_000_000);
        long oldUserId = createPasswordUser(account, "Framework2026", true, true, true, email, phone);

        userService.delete(oldUserId);
        redissonCache.putString(RedisKeyPrefix.EMAIL_REGISTER + email, "123456", 300);
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginCode":"%s","loginName":"重新注册用户","password":"Framework2026",
                                 "linkPhone":"%s","email":"%s","captcha":"123456"}
                                """.formatted(account, phone, email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertEquals(2L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE email = ?", Long.class, email));
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE email = ? AND is_delete = 0", Long.class, email));
        assertEquals(2L, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM user_identity
                WHERE identity_type = 'password' AND identifier = ?
                """, Long.class, account));
    }

    @Test
    void shouldChangePasswordInvalidateOldSessionAndUseNewCredential() throws Exception {
        String account = "change-" + UUID.randomUUID().toString().substring(0, 8);
        createPasswordUser(account, "Framework2026", true, true, true);
        String oldToken = loginAndGetToken(account, "Framework2026");

        mockMvc.perform(post("/user/change-password")
                        .header("Authorization", "Bearer " + oldToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"Framework2026","newPassword":"Changed2026Pwd"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/user/info")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(account, "Framework2026")))
                .andExpect(status().isUnauthorized());
        loginAndGetToken(account, "Changed2026Pwd");
    }

    @Test
    void shouldAllowOnlyOneConcurrentPasswordResetAndInvalidateExistingSession() throws Exception {
        String account = "reset-" + UUID.randomUUID().toString().substring(0, 8);
        String email = account + "@example.com";
        createPasswordUser(account, "Framework2026", true, true, true);
        String oldToken = loginAndGetToken(account, "Framework2026");

        mockMvc.perform(post("/user/reset-password-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk());
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailUtils).baseSendMail(eq(email), anyString(), contentCaptor.capture(), anyBoolean());
        String resetToken = extractResetToken(contentCaptor.getValue());
        assertTrue(SaTempUtil.getTimeout(resetToken) > 0,
                "邮件中的密码重置 Token 应已写入 Redis 且尚未过期，token=" + resetToken);

        String resetBody = """
                {"newPassword":"Reset2026Password","token":"%s"}
                """.formatted(resetToken);
        List<MvcResult> results = executeConcurrently(2, () -> mockMvc.perform(post("/user/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody))
                .andReturn());
        long successCount = results.stream()
                .filter(result -> result.getResponse().getStatus() == 200)
                .count();
        assertEquals(1, successCount, "同一个密码重置 Token 只能有一个并发请求成功");

        mockMvc.perform(get("/user/info")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
        loginAndGetToken(account, "Reset2026Password");
    }

    @Test
    void shouldBindSingleAlipayIdentityDuringConcurrentFirstLogin() throws Exception {
        String alipayUserId = "2088" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        when(alipayClient.execute(any(AlipaySystemOauthTokenRequest.class)))
                .thenAnswer(invocation -> successfulAlipayResponse(alipayUserId));

        List<MvcResult> results = executeConcurrently(2, () -> mockMvc.perform(post("/ali-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authCode\":\"integration-auth-code\"}"))
                .andReturn());
        assertTrue(results.stream().allMatch(result -> result.getResponse().getStatus() == 200),
                "两个并发首次登录请求都应收敛到同一个支付宝身份");
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM user_identity
                WHERE identity_type = 'alipay' AND identifier = ?
                """, Integer.class, alipayUserId));
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT user_id) FROM user_identity
                WHERE identity_type = 'alipay' AND identifier = ?
                """, Integer.class, alipayUserId));

        String firstToken = JsonPath.read(results.get(0).getResponse().getContentAsString(), "$.data.token");
        String secondToken = JsonPath.read(results.get(1).getResponse().getContentAsString(), "$.data.token");
        assertNotEquals(firstToken, secondToken, "每次登录应签发独立 Token");
    }

    @Test
    void shouldInvalidateUserSessionAfterRoleAndPermissionReplacement() throws Exception {
        String roleAccount = "role-" + UUID.randomUUID().toString().substring(0, 8);
        long roleChangedUserId = createPasswordUser(
                roleAccount, "Framework2026", true, true, true);
        String roleChangedToken = loginAndGetToken(roleAccount, "Framework2026");
        Integer memberRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM rbac_role WHERE scope = 'business' AND code = 'business:member'",
                Integer.class);
        subjectRoleService.replaceRoles(
                RbacSubjectType.BUSINESS_USER.code(), roleChangedUserId, List.of(memberRoleId));
        assertUserTokenInvalid(roleChangedToken);

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String roleCode = "business:test:" + suffix;
        String permissionCode = roleCode + ":read";
        jdbcTemplate.update("""
                INSERT INTO rbac_role
                    (scope, code, name, remark, built_in, super_role, is_delete,
                     update_time, create_time)
                VALUES ('business', ?, '集成测试角色', '仅用于集成测试', 0, 0, 0,
                        NOW(), NOW())
                """, roleCode);
        jdbcTemplate.update("""
                INSERT INTO rbac_permission
                    (scope, code, name, resource_type, description, status, is_delete)
                VALUES ('business', ?, '集成测试权限', 'api', '仅用于集成测试', 1, 0)
                """, permissionCode);
        Integer customRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM rbac_role WHERE scope = 'business' AND code = ?",
                Integer.class, roleCode);
        Integer customPermissionId = jdbcTemplate.queryForObject(
                "SELECT id FROM rbac_permission WHERE scope = 'business' AND code = ?",
                Integer.class, permissionCode);
        rolePermissionService.replacePermissions(customRoleId, List.of(customPermissionId));

        String permissionAccount = "permission-" + suffix;
        long permissionChangedUserId = createPasswordUser(
                permissionAccount, "Framework2026", true, true, true);
        subjectRoleService.replaceRoles(
                RbacSubjectType.BUSINESS_USER.code(), permissionChangedUserId,
                List.of(customRoleId));
        String permissionToken = loginAndGetToken(permissionAccount, "Framework2026");
        rolePermissionService.replacePermissions(customRoleId, List.of());
        assertUserTokenInvalid(permissionToken);
    }

    private long createPasswordUser(String loginCode,
                                    String password,
                                    boolean identityStatus,
                                    boolean verified,
                                    boolean userStatus) {
        return createPasswordUser(loginCode, password, identityStatus, verified, userStatus,
                loginCode + "@example.com", null);
    }

    private long createPasswordUser(String loginCode,
                                    String password,
                                    boolean identityStatus,
                                    boolean verified,
                                    boolean userStatus,
                                    String email,
                                    String phone) {
        jdbcTemplate.update("""
                INSERT INTO `user`
                    (display_name, status, email, phone, update_time, create_time,
                     create_uid, update_uid, is_delete)
                VALUES ('集成用户', ?, ?, ?, NOW(), NOW(), 0, 0, 0)
                """, userStatus, email, phone);
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM `user` WHERE email = ? AND is_delete = 0", Long.class, email);
        jdbcTemplate.update("""
                INSERT INTO user_identity
                    (user_id, identity_type, identifier, credential, verified, status)
                VALUES (?, 'password', ?, ?, ?, ?)
                """, userId, loginCode, passwordEncoder.encode(password), verified, identityStatus);
        jdbcTemplate.update("""
                INSERT INTO rbac_subject_role
                    (subject_type, subject_id, scope, role_id, update_time, create_time)
                SELECT 'user', ?, 'business', id, NOW(), NOW()
                FROM rbac_role
                WHERE scope = 'business' AND code = 'business:member'
                """, userId);
        return userId;
    }

    private String loginJson(String account, String password) {
        return """
                {"loginCode":"%s","password":"%s"}
                """.formatted(account, password);
    }

    private String loginAndGetToken(String account, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(account, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.data.token");
    }

    private void assertUserTokenInvalid(String token) throws Exception {
        mockMvc.perform(get("/user/info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private String extractResetToken(String mailContent) {
        Matcher matcher = RESET_TOKEN_PATTERN.matcher(mailContent);
        assertTrue(matcher.find(), "重置密码邮件应包含一次性 Token");
        return matcher.group(1).trim();
    }

    private AlipaySystemOauthTokenResponse successfulAlipayResponse(String alipayUserId) {
        AlipaySystemOauthTokenResponse response = new AlipaySystemOauthTokenResponse();
        response.setCode("10000");
        response.setUserId(alipayUserId);
        return response;
    }

    /**
     * 让多个任务在同一时刻进入被测入口，避免顺序调用掩盖 Redis 预占和数据库唯一键竞态。
     */
    private <T> List<T> executeConcurrently(int taskCount, ConcurrentTask<T> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(taskCount);
        CountDownLatch ready = new CountDownLatch(taskCount);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int index = 0; index < taskCount; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.execute();
                }));
            }
            ready.await();
            start.countDown();
            List<T> results = new ArrayList<>(taskCount);
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface ConcurrentTask<T> {
        T execute() throws Exception;
    }
}
