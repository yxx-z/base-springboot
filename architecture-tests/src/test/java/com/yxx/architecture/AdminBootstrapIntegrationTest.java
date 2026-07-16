package com.yxx.architecture;

import com.yxx.admin.bootstrap.AdminBootstrapConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** 使用真实数据库验证 bootstrap 最小上下文只创建初始管理员并在提交后退出。 */
@Testcontainers(disabledWithoutDocker = true)
class AdminBootstrapIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("admin_bootstrap_it")
            .withUsername("integration")
            .withPassword("integration");

    @Test
    void shouldCreateInitialSuperAdminAndCloseMinimalContext() throws Exception {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(
                AdminBootstrapConfiguration.class)
                .web(WebApplicationType.NONE)
                .run(
                        "--spring.profiles.active=bootstrap",
                        "--spring.datasource.url=" + MYSQL.getJdbcUrl(),
                        "--spring.datasource.username=" + MYSQL.getUsername(),
                        "--spring.datasource.password=" + MYSQL.getPassword(),
                        "--spring.flyway.locations=classpath:db/migration/admin",
                        "--spring.flyway.table=flyway_schema_history_admin",
                        "--bootstrap.admin.login-code=BootstrapAdmin",
                        "--bootstrap.admin.login-name=初始管理员",
                        "--bootstrap.admin.email=bootstrap@example.com",
                        "--bootstrap.admin.password=Framework2026");

        waitUntilClosed(context, Duration.ofSeconds(5));
        assertFalse(context.isActive(), "bootstrap 完成事务后应主动关闭最小上下文");
        assertEquals(1L, count("SELECT COUNT(*) FROM admin_user"));
        assertEquals("bootstrapadmin", queryString(
                "SELECT login_code FROM admin_user LIMIT 1"));
        assertTrue(new BCryptPasswordEncoder().matches(
                "Framework2026", queryString("SELECT password FROM admin_user LIMIT 1")));
        assertEquals(1L, count("""
                SELECT COUNT(*) FROM admin_user_role ur
                JOIN admin_role r ON r.id = ur.role_id
                WHERE r.code = 'admin:super-admin'
                """));
    }

    private void waitUntilClosed(ConfigurableApplicationContext context, Duration timeout)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (context.isActive() && System.nanoTime() < deadline) {
            Thread.sleep(25L);
        }
        if (context.isActive()) {
            context.close();
            fail("bootstrap 上下文未在限定时间内退出");
        }
    }

    private long count(String sql) throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private String queryString(String sql) throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }
}
