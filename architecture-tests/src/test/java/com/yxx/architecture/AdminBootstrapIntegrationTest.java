package com.yxx.architecture;

import com.yxx.admin.bootstrap.AdminBootstrapConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** 使用真实数据库验证 bootstrap 最小上下文只创建初始管理员并在提交后退出。 */
@Testcontainers(disabledWithoutDocker = true)
class AdminBootstrapIntegrationTest {

    @Container
    private static final MySQLContainer MYSQL = new MySQLContainer(
            DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("admin_bootstrap_it")
            .withUsername("integration")
            .withPassword("integration");

    @Test
    void shouldCreateInitialSuperAdminAndCloseMinimalContext() throws Exception {
        CountDownLatch contextClosedSignal = new CountDownLatch(1);
        AtomicReference<Thread> contextClosingThread = new AtomicReference<>();
        ConfigurableApplicationContext context = new SpringApplicationBuilder(
                AdminBootstrapConfiguration.class)
                .web(WebApplicationType.NONE)
                .listeners((ApplicationListener<ContextClosedEvent>) event -> {
                    contextClosingThread.set(Thread.currentThread());
                    contextClosedSignal.countDown();
                })
                .run(
                        "--spring.profiles.active=bootstrap",
                        "--spring.datasource.url=" + MYSQL.getJdbcUrl(),
                        "--spring.datasource.username=" + MYSQL.getUsername(),
                        "--spring.datasource.password=" + MYSQL.getPassword(),
                        "--spring.flyway.locations=classpath:db/migration/shared",
                        "--spring.flyway.table=flyway_schema_history",
                        "--bootstrap.admin.login-code=BootstrapAdmin",
                        "--bootstrap.admin.login-name=初始管理员",
                        "--bootstrap.admin.email=bootstrap@example.com",
                        "--bootstrap.admin.password=Framework2026");

        waitUntilClosed(context, contextClosedSignal, contextClosingThread,
                Duration.ofSeconds(5));
        assertFalse(context.isActive(), "bootstrap 完成事务后应主动关闭最小上下文");
        assertEquals(1L, count("SELECT COUNT(*) FROM admin_user"));
        assertEquals("bootstrapadmin", queryString(
                "SELECT login_code FROM admin_user LIMIT 1"));
        assertTrue(new BCryptPasswordEncoder().matches(
                "Framework2026", queryString("SELECT password FROM admin_user LIMIT 1")));
        assertEquals(1L, count("""
                SELECT COUNT(*) FROM rbac_subject_role ur
                JOIN rbac_role r ON r.id = ur.role_id AND r.scope = ur.scope
                WHERE ur.subject_type = 'admin'
                  AND r.scope = 'admin'
                  AND r.code = 'admin:super-admin'
                """));
    }

    /**
     * 等待 bootstrap 线程主动关闭 Spring 上下文。
     *
     * <p>上下文关闭事件只表示关闭流程已经开始，因此收到事件后还需要等待实际执行
     * {@code close()} 的线程结束，才能断言上下文已经完全关闭。整个过程使用事件信号和线程
     * 终止通知进行阻塞等待，避免通过循环休眠轮询状态。</p>
     */
    private void waitUntilClosed(ConfigurableApplicationContext context,
                                 CountDownLatch contextClosedSignal,
                                 AtomicReference<Thread> contextClosingThread,
                                 Duration timeout)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        if (!contextClosedSignal.await(timeout.toNanos(), TimeUnit.NANOSECONDS)) {
            context.close();
            fail("bootstrap 上下文未在限定时间内退出");
        }

        Thread closingThread = contextClosingThread.get();
        long remainingNanos = deadline - System.nanoTime();
        if (closingThread == null || remainingNanos <= 0L) {
            context.close();
            fail("bootstrap 上下文未在限定时间内完成关闭");
        }
        long remainingMillis = TimeUnit.NANOSECONDS.toMillis(remainingNanos);
        int remainingNanoPart = (int) (remainingNanos
                - TimeUnit.MILLISECONDS.toNanos(remainingMillis));
        closingThread.join(remainingMillis, remainingNanoPart);
        if (closingThread.isAlive()) {
            context.close();
            fail("bootstrap 上下文未在限定时间内完成关闭");
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
