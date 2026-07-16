package com.yxx.architecture;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证用户端与管理端临时共用同一 Schema 时，两套迁移和历史表仍保持相互独立。
 */
@Testcontainers(disabledWithoutDocker = true)
class SharedDatabaseFlywayIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("shared_app_it")
            .withUsername("integration")
            .withPassword("integration");

    @Test
    void shouldMigrateBusinessAndAdminIntoSharedSchemaWithIndependentHistories()
            throws SQLException {
        Flyway business = flyway(
                "classpath:db/migration/business", "flyway_schema_history_business");
        Flyway admin = flyway(
                "classpath:db/migration/admin", "flyway_schema_history_admin");

        assertEquals(2, business.migrate().migrationsExecuted);
        assertEquals(2, admin.migrate().migrationsExecuted);
        assertEquals(0, business.migrate().migrationsExecuted);
        assertEquals(0, admin.migrate().migrationsExecuted);

        assertTrue(tableExists("user"));
        assertTrue(tableExists("admin_user"));
        assertEquals(2L, count("SELECT COUNT(*) FROM flyway_schema_history_business WHERE success = 1"));
        assertEquals(2L, count("""
                SELECT COUNT(*) FROM flyway_schema_history_admin
                WHERE success = 1 AND version IN ('1', '2')
                """));
        assertEquals(1L, count("""
                SELECT COUNT(*) FROM flyway_schema_history_admin
                WHERE success = 1 AND version = '0' AND type = 'BASELINE'
                """));
    }

    private Flyway flyway(String location, String historyTable) {
        return Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations(location)
                .table(historyTable)
                // 第二个应用面对非空共享 Schema 时先登记 0，再完整执行自己的 V1、V2。
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
    }

    private boolean tableExists(String tableName) throws SQLException {
        return count("SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = DATABASE() AND table_name = '" + tableName + "'") == 1L;
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
