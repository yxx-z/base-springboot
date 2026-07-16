package com.yxx.admin.integration;

import com.jayway.jsonpath.JsonPath;
import com.yxx.admin.AdminApplication;
import com.yxx.admin.mapper.OperateAdminLogMapper;
import com.yxx.admin.model.request.OperateLogReq;
import com.yxx.admin.model.response.OperateLogResp;
import com.yxx.admin.service.AdminUserRoleService;
import com.yxx.admin.service.AdminUserService;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.common.utils.email.MailUtils;
import com.yxx.rbac.model.RbacMenuNode;
import com.yxx.rbac.model.RbacScope;
import com.yxx.rbac.model.RbacSubjectType;
import com.yxx.rbac.service.RbacRoleMenuService;
import com.yxx.rbac.service.RbacRolePermissionService;
import com.yxx.rbac.service.RbacSubjectRoleService;
import com.yxx.security.authorization.AuthorizationProvider;
import com.yxx.security.constant.SecurityRealm;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 使用真实 MySQL、Redis 和完整 Spring 上下文验证管理端启动、会话与 RBAC 安全不变量。
 */
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("integration")
@SpringBootTest(classes = AdminApplication.class, properties = {
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
        "sa-token.token-name=Authorization",
        "sa-token.token-prefix=Bearer",
        "sa-token.timeout=3600",
        "sa-token.active-timeout=1800",
        "sa-token.is-concurrent=false",
        "sa-token.is-share=false",
        "sa-token.token-style=uuid",
        "sa-token.is-log=false",
        "sa-token.is-print=false"
})
@AutoConfigureMockMvc
class AdminApplicationIntegrationTest {

    @Container
    private static final MySQLContainer MYSQL = new MySQLContainer(
            DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("admin_app_it")
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
    private MailUtils mailUtils;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private AdminUserRoleService adminUserRoleService;

    @Autowired
    private RbacRolePermissionService rolePermissionService;

    @Autowired
    private RbacRoleMenuService roleMenuService;

    @Autowired
    private AuthorizationProvider authorizationProvider;

    @Autowired
    private RbacSubjectRoleService subjectRoleService;

    @Autowired
    private OperateAdminLogMapper operateAdminLogMapper;

    @BeforeEach
    void cleanAdminUsers() {
        jdbcTemplate.update("DELETE FROM operate_admin_log");
        jdbcTemplate.update("DELETE FROM rbac_subject_role WHERE subject_type = 'admin'");
        jdbcTemplate.update("DELETE FROM admin_user");
    }

    @Test
    void shouldStartLoginPersistAdminSessionAndExecuteMapperXml() throws Exception {
        long adminId = createAdmin("root-admin", true, "admin:super-admin");
        String token = loginAndGetToken(" ROOT-ADMIN ", "Framework2026");

        mockMvc.perform(get("/user/info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(adminId))
                .andExpect(jsonPath("$.data.permissions[0]").value("*"));

        String sessionKeyCount = REDIS.execInContainer(
                "redis-cli", "-n", "1", "DBSIZE").getStdout().trim();
        assertTrue(Integer.parseInt(sessionKeyCount) > 0, "管理员登录态应写入独立 Session Redis");

        jdbcTemplate.update("""
                INSERT INTO operate_admin_log
                    (user_id, actor_type, actor_account, actor_name, subject_account,
                     type, event_type, module, title, create_time, is_delete)
                VALUES (?, 'admin', 'root-admin', '根管理员', NULL,
                        1, 'AUTHENTICATION', '测试', '登录', NOW(), 0)
                """, adminId);
        Page<OperateLogResp> page = operateAdminLogMapper.authLogPage(
                new Page<>(1, 10), new OperateLogReq());
        assertFalse(page.getRecords().isEmpty());
        assertEquals("root-admin", page.getRecords().get(0).getLoginCode());
    }

    @Test
    void shouldInvalidateHistoricalSessionWhenAdminIsEnabledAgain() throws Exception {
        long adminId = createAdmin("reenabled-admin", true, "admin:administrator");
        String historicalToken = loginAndGetToken("reenabled-admin", "Framework2026");

        /*
         * 直接更新数据库模拟“停用时 Redis 注销失败，旧 Token 仍然残留”的极端状态。
         * 随后通过正式服务重新启用账号，必须再次注销全部历史会话。
         */
        jdbcTemplate.update("UPDATE admin_user SET status = 0 WHERE id = ?", adminId);
        adminUserService.changeStatus(adminId, true);

        assertTokenInvalid(historicalToken);
    }

    @Test
    void shouldInvalidateSessionAfterRoleAndPermissionReplacement() throws Exception {
        long roleChangedAdminId = createAdmin("role-change", true, "admin:administrator");
        String roleChangedToken = loginAndGetToken("role-change", "Framework2026");
        Integer administratorRoleId = roleId("admin:administrator");
        adminUserRoleService.replaceRoles(roleChangedAdminId, List.of(administratorRoleId));
        assertTokenInvalid(roleChangedToken);

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Integer customRoleId = createRole("admin:test:" + suffix);
        Integer customPermissionId = createPermission("admin:test:" + suffix + ":read");
        rolePermissionService.replacePermissions(customRoleId, List.of(customPermissionId));
        long permissionChangedAdminId = createAdmin("permission-" + suffix, true, null);
        adminUserRoleService.replaceRoles(permissionChangedAdminId, List.of(customRoleId));
        String permissionToken = loginAndGetToken("permission-" + suffix, "Framework2026");
        assertTrue(authorizationProvider.load(SecurityRealm.ADMIN, permissionChangedAdminId)
                .permissions()
                .contains("admin:test:" + suffix + ":read"));

        rolePermissionService.replacePermissions(customRoleId, List.of());
        assertTokenInvalid(permissionToken);
    }

    @Test
    void shouldProtectLastSuperAdminAndBuiltInRole() {
        long superAdminId = createAdmin("only-super", true, "admin:super-admin");
        ApiException disableException = assertThrows(
                ApiException.class, () -> adminUserService.changeStatus(superAdminId, false));
        assertEquals(ApiCode.LAST_SUPER_ADMIN.code(), disableException.getCode());

        Integer superRoleId = roleId("admin:super-admin");
        ApiException immutableException = assertThrows(ApiException.class,
                () -> rolePermissionService.replacePermissions(superRoleId, List.of()));
        assertEquals(ApiCode.BUILT_IN_ROLE_IMMUTABLE.code(), immutableException.getCode());
        assertEquals(List.of("*"), authorizationProvider
                .load(SecurityRealm.ADMIN, superAdminId).permissions().stream().toList());
    }

    @Test
    void shouldKeepOneSuperAdminUnderConcurrentDisableRequests() throws Exception {
        long firstAdminId = createAdmin("concurrent-super-1", true, "admin:super-admin");
        long secondAdminId = createAdmin("concurrent-super-2", true, "admin:super-admin");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> first = executor.submit(() -> disableConcurrently(firstAdminId, ready, start));
            Future<Integer> second = executor.submit(() -> disableConcurrently(secondAdminId, ready, start));
            ready.await();
            start.countDown();
            List<Integer> results = List.of(first.get(), second.get());
            assertEquals(1, results.stream().filter(code -> code == 0).count());
            assertEquals(1, results.stream()
                    .filter(code -> ApiCode.LAST_SUPER_ADMIN.code().equals(code)).count());
            assertEquals(1L, jdbcTemplate.queryForObject("""
                    SELECT COUNT(DISTINCT au.id)
                    FROM admin_user au
                    JOIN rbac_subject_role relation
                      ON relation.subject_type = 'admin' AND relation.subject_id = au.id
                    JOIN rbac_role role ON role.id = relation.role_id
                    WHERE au.is_delete = 0 AND au.status = 1
                      AND role.code = 'admin:super-admin' AND role.is_delete = 0
                    """, Long.class));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldProvideCompleteAdminAndBusinessUserManagement() throws Exception {
        createAdmin("management-root", true, "admin:super-admin");
        String token = loginAndGetToken("management-root", "Framework2026");
        Integer administratorRoleId = roleId("admin:administrator");

        String createResponse = mockMvc.perform(post("/management/admin-users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginCode":"managed-admin",
                                  "loginName":"受管管理员",
                                  "password":"Framework2026",
                                  "email":"managed-admin@example.com",
                                  "linkPhone":"13800138000",
                                  "roleIds":[%d]
                                }
                                """.formatted(administratorRoleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        Long managedAdminId = ((Number) JsonPath.read(createResponse, "$.data")).longValue();

        mockMvc.perform(put("/management/admin-users/{userId}/status", managedAdminId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/management/admin-users/{userId}", managedAdminId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_user WHERE id = ? AND is_delete = 1",
                Long.class, managedAdminId));

        long businessUserId = createBusinessUser("deletable-business@example.com");
        mockMvc.perform(put("/management/business-users/{userId}/status", businessUserId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/management/business-users/{userId}", businessUserId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE id = ? AND is_delete = 1",
                Long.class, businessUserId));
    }

    @Test
    void shouldCreateConfigureAndDeleteRbacResourcesThroughAdminApi() throws Exception {
        createAdmin("rbac-manager", true, "admin:super-admin");
        String token = loginAndGetToken("rbac-manager", "Framework2026");
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Integer roleId = responseInteger(mockMvc.perform(post("/management/rbac/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scope":"admin","code":"admin:test:%s","name":"测试角色","remark":"集成测试"}
                                """.formatted(suffix)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        Integer permissionId = responseInteger(mockMvc.perform(post("/management/rbac/permissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scope":"admin","code":"admin:test:%s:read","name":"测试权限",
                                 "resourceType":"api","description":"集成测试","enabled":true}
                                """.formatted(suffix)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        Integer menuId = responseInteger(mockMvc.perform(post("/management/rbac/menus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scope":"admin","menuCode":"admin-test-%s","menuName":"测试菜单",
                                 "path":"/test/%s","component":"test/Index","sort":500,
                                 "visible":true,"enabled":true}
                                """.formatted(suffix, suffix)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        mockMvc.perform(put("/management/rbac/roles/{roleId}/permissions", roleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissionIds\":[%d]}".formatted(permissionId)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/management/rbac/roles/{roleId}/menus", roleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menuIds\":[%d]}".formatted(menuId)))
                .andExpect(status().isOk());

        // 角色删除会先清理权限和菜单关联；资源随后可分别软删。
        mockMvc.perform(delete("/management/rbac/roles/{roleId}", roleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/management/rbac/permissions/{permissionId}", permissionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/management/rbac/menus/{menuId}", menuId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldManageBusinessUserRolesAndRejectCrossScopeAssignment() throws Exception {
        createAdmin("business-manager", true, "admin:super-admin");
        String token = loginAndGetToken("business-manager", "Framework2026");
        long businessUserId = createBusinessUser("managed-business@example.com");
        Integer memberRoleId = jdbcTemplate.queryForObject("""
                SELECT id FROM rbac_role
                WHERE scope = 'business' AND code = 'business:member'
                """, Integer.class);
        Integer adminRoleId = roleId("admin:administrator");

        mockMvc.perform(put("/management/business-users/{userId}/roles", businessUserId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roleIds":[%d]}
                                """.formatted(memberRoleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/management/business-users/{userId}", businessUserId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(businessUserId))
                .andExpect(jsonPath("$.data.roleIds[0]").value(memberRoleId));

        ApiException exception = assertThrows(ApiException.class,
                () -> subjectRoleService.replaceRoles(
                        RbacSubjectType.BUSINESS_USER.code(), businessUserId,
                        List.of(adminRoleId)));
        assertEquals(ApiCode.RBAC_SCOPE_MISMATCH.code(), exception.getCode());
    }

    @Test
    void shouldPromoteVisibleChildOfHiddenParentAndRejectCyclicBranch() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Integer hiddenParentId = insertMenu(null, "hidden-" + suffix, false, 10);
        Integer visibleChildId = insertMenu(hiddenParentId, "visible-child-" + suffix, true, 11);

        List<RbacMenuNode> menus = assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> roleMenuService.currentMenuTree(
                        RbacScope.ADMIN, List.of("admin:super-admin")));
        assertTrue(menus.stream().anyMatch(menu -> ("visible-child-" + suffix).equals(menu.getCode())),
                "隐藏父菜单的可见子菜单应提升为可导航节点");

        Integer cycleA = insertMenu(null, "cycle-a-" + suffix, true, 20);
        Integer cycleB = insertMenu(cycleA, "cycle-b-" + suffix, true, 21);
        jdbcTemplate.update("UPDATE rbac_menu SET parent_id = ? WHERE id = ?", cycleB, cycleA);
        try {
            IllegalStateException exception = assertTimeoutPreemptively(Duration.ofSeconds(2),
                    () -> assertThrows(IllegalStateException.class,
                            () -> roleMenuService.currentMenuTree(
                                    RbacScope.ADMIN, List.of("admin:super-admin"))));
            assertTrue(exception.getMessage().contains("循环父子关系"));
        } finally {
            // 恢复循环关系后再删除测试数据，避免污染同一容器中的其他测试。
            jdbcTemplate.update("UPDATE rbac_menu SET parent_id = NULL WHERE id = ?", cycleA);
            jdbcTemplate.update("DELETE FROM rbac_menu WHERE id = ?", cycleB);
            jdbcTemplate.update("DELETE FROM rbac_menu WHERE id = ?", cycleA);
            jdbcTemplate.update("DELETE FROM rbac_menu WHERE id = ?", visibleChildId);
            jdbcTemplate.update("DELETE FROM rbac_menu WHERE id = ?", hiddenParentId);
        }
    }

    private long createAdmin(String loginCode, boolean enabled, String roleCode) {
        String email = loginCode + "@example.com";
        jdbcTemplate.update("""
                INSERT INTO admin_user
                    (login_code, login_name, password, status, email, update_time, create_time,
                     create_uid, update_uid, is_delete)
                VALUES (?, '集成管理员', ?, ?, ?, NOW(), NOW(), 0, 0, 0)
                """, loginCode, passwordEncoder.encode("Framework2026"), enabled, email);
        Long adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM admin_user WHERE login_code = ?", Long.class, loginCode);
        if (roleCode != null) {
            jdbcTemplate.update("""
                    INSERT INTO rbac_subject_role
                        (subject_type, subject_id, scope, role_id, update_time, create_time)
                    SELECT 'admin', ?, 'admin', id, NOW(), NOW()
                    FROM rbac_role WHERE scope = 'admin' AND code = ?
                    """, adminId, roleCode);
        }
        return adminId;
    }

    private int disableConcurrently(Long adminId, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            start.await();
            adminUserService.changeStatus(adminId, false);
            return 0;
        } catch (ApiException exception) {
            return exception.getCode();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private long createBusinessUser(String email) {
        jdbcTemplate.update("""
                INSERT INTO `user`
                    (display_name, status, email, update_time, create_time,
                     create_uid, update_uid, is_delete)
                VALUES ('被管理业务用户', 1, ?, NOW(), NOW(), 0, 0, 0)
                """, email);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM `user` WHERE email = ?", Long.class, email);
    }

    private String loginAndGetToken(String account, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginCode":"%s","password":"%s"}
                                """.formatted(account, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.data.token");
    }

    private Integer responseInteger(String response) {
        return ((Number) JsonPath.read(response, "$.data")).intValue();
    }

    private void assertTokenInvalid(String token) throws Exception {
        mockMvc.perform(get("/user/info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private Integer roleId(String roleCode) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM rbac_role WHERE scope = 'admin' AND code = ?",
                Integer.class, roleCode);
    }

    private Integer createRole(String code) {
        jdbcTemplate.update("""
                INSERT INTO rbac_role
                    (scope, code, name, remark, built_in, super_role, is_delete,
                     update_time, create_time)
                VALUES ('admin', ?, '集成测试角色', '仅用于集成测试', 0, 0, 0,
                        NOW(), NOW())
                """, code);
        return roleId(code);
    }

    private Integer createPermission(String code) {
        jdbcTemplate.update("""
                INSERT INTO rbac_permission
                    (scope, code, name, resource_type, description, status, is_delete)
                VALUES ('admin', ?, '集成测试权限', 'api', '仅用于集成测试', 1, 0)
                """, code);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM rbac_permission WHERE scope = 'admin' AND code = ?",
                Integer.class, code);
    }

    private Integer insertMenu(Integer parentId, String code, boolean visible, int sort) {
        jdbcTemplate.update("""
                INSERT INTO rbac_menu
                    (scope, parent_id, menu_code, menu_name, path, component, sort,
                     visible, status, is_delete)
                VALUES ('admin', ?, ?, ?, ?, 'test/Index', ?, ?, 1, 0)
                """, parentId, code, code, "/" + code, sort, visible);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM rbac_menu WHERE scope = 'admin' AND menu_code = ?",
                Integer.class, code);
    }

}
