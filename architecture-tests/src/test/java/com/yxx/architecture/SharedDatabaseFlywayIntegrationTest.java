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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 在真实 MySQL 上验证 admin、business 共用的完整 Schema 迁移。
 *
 * <p>两个应用依赖同一个迁移制品和同一张历史表，所以无论哪个应用先启动，都能获得
 * 管理端账号、业务用户和统一 RBAC 所需的完整表结构。</p>
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
    void shouldMigrateEmptySchemaAndUpgradeFromV1ToLatest() throws SQLException {
        Flyway v1 = flyway("1");
        v1.clean();
        assertEquals(1, v1.migrate().migrationsExecuted);

        assertTrue(tableExists("user"));
        assertTrue(tableExists("admin_user"));
        assertTrue(tableExists("rbac_role"));
        assertTrue(tableExists("rbac_subject_role"));
        assertFalse(columnExists("operate_log", "actor_account"));
        assertFalse(columnExists("operate_admin_log", "actor_account"));
        assertEquals(1L, count("""
                SELECT COUNT(*) FROM rbac_role
                WHERE scope = 'business' AND code = 'business:member'
                """));
        assertEquals(1L, count("""
                SELECT COUNT(*) FROM rbac_role
                WHERE scope = 'admin' AND code = 'admin:super-admin'
                """));
        assertThrows(SQLException.class, () -> executeUpdate("""
                INSERT INTO rbac_subject_role
                    (subject_type, subject_id, scope, role_id, update_time, create_time)
                SELECT 'user', 1, 'admin', id, NOW(), NOW()
                FROM rbac_role
                WHERE scope = 'admin' AND code = 'admin:administrator'
                """), "数据库 CHECK 约束必须拒绝业务用户关联管理端角色");
        assertThrows(SQLException.class, () -> executeUpdate("""
                INSERT INTO rbac_role_permission (scope, role_id, permission_id)
                SELECT 'admin', r.id, p.id
                FROM rbac_role r
                JOIN rbac_permission p ON p.scope = 'business'
                WHERE r.scope = 'admin' AND r.code = 'admin:administrator'
                LIMIT 1
                """), "复合外键必须拒绝跨权限域角色权限关联");

        Flyway latest = flyway(null);
        assertEquals(1, latest.migrate().migrationsExecuted);
        assertEquals(0, latest.migrate().migrationsExecuted);
        assertTrue(columnExists("operate_log", "actor_account"));
        assertTrue(columnExists("operate_admin_log", "actor_account"));
        assertEquals(2L, count("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE success = 1 AND version IN ('1', '2')
                """));
    }

    private Flyway flyway(String target) {
        var configuration = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration/shared")
                .table("flyway_schema_history")
                .cleanDisabled(false);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private boolean tableExists(String tableName) throws SQLException {
        return count("SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = DATABASE() AND table_name = '" + tableName + "'") == 1L;
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

    private void executeUpdate(String sql) throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
