package com.yxx.admin.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 在真实 MySQL 上验证管理端 Flyway 全量迁移和版本升级。 */
@Testcontainers(disabledWithoutDocker = true)
class AdminFlywayIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("admin_it")
            .withUsername("integration")
            .withPassword("integration");

    @Test
    void shouldMigrateFromV1ToLatestAndKeepSecuritySeedData() throws SQLException {
        Flyway v1 = flyway("1");
        v1.clean();
        assertEquals(1, v1.migrate().migrationsExecuted);
        assertTrue(tableExists("admin_user_role"));
        assertFalse(columnExists("operate_admin_log", "actor_account"));

        Flyway latest = flyway(null);
        assertEquals(1, latest.migrate().migrationsExecuted);
        assertTrue(columnExists("operate_admin_log", "actor_account"));
        assertTrue(columnExists("operate_admin_log", "subject_account"));
        assertEquals(1L, count("SELECT COUNT(*) FROM admin_role WHERE code = 'admin:super-admin'"));
        assertEquals(2L, count("SELECT COUNT(*) FROM flyway_schema_history_admin WHERE success = 1"));
    }

    private Flyway flyway(String target) {
        var configuration = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration/admin")
                .table("flyway_schema_history_admin")
                .cleanDisabled(false);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private boolean tableExists(String tableName) throws SQLException {
        return count("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() "
                + "AND table_name = '" + tableName + "'") == 1L;
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        return count("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = '" + tableName + "' AND column_name = '" + columnName + "'") == 1L;
    }

    private long count(String sql) throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
