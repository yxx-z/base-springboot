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
    void shouldMigrateEmptySchemaToFinalBaseline() throws SQLException {
        Flyway flyway = flyway();
        flyway.clean();
        assertEquals(1, flyway.migrate().migrationsExecuted);
        // 最终基线已经完整落库，重复执行迁移不应产生额外变更。
        assertEquals(0, flyway.migrate().migrationsExecuted);

        assertTrue(tableExists("user"));
        assertTrue(tableExists("admin_user"));
        assertTrue(tableExists("rbac_role"));
        assertTrue(tableExists("rbac_subject_role"));
        assertTrue(columnExists("operate_log", "actor_account"));
        assertTrue(columnExists("operate_admin_log", "actor_account"));
        assertTrue(columnExists("user", "active_email"));
        assertTrue(columnExists("user_identity", "active_identifier"));
        assertTrue(columnExists("operate_admin_log", "subject_id"));
        assertEquals(1L, count("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE success = 1 AND version = '1'
                """));
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
        // 初始基线不再创建已经废弃的业务端全局审计权限，先构造一个合法的业务权限，
        // 再验证 admin 角色无法通过伪造 scope 关联 business 权限。
        executeUpdate("""
                INSERT INTO rbac_permission
                    (scope, code, name, resource_type, description, status, is_delete)
                VALUES ('business', 'business:test:read', '测试业务权限', 'api',
                        '仅用于验证权限域复合外键', 1, 0)
                """);
        assertThrows(SQLException.class, () -> executeUpdate("""
                INSERT INTO rbac_role_permission (scope, role_id, permission_id)
                SELECT 'admin', r.id, p.id
                FROM rbac_role r
                JOIN rbac_permission p ON p.scope = 'business'
                WHERE r.scope = 'admin' AND r.code = 'admin:administrator'
                LIMIT 1
                """), "复合外键必须拒绝跨权限域角色权限关联");
        assertEquals(0L, count("""
                SELECT COUNT(*) FROM rbac_permission
                WHERE scope = 'business' AND code = 'business:audit-log:read'
                  AND is_delete = 0
                """));

        executeUpdate("""
                INSERT INTO `user`
                    (display_name, status, phone, email, update_time, create_time,
                     create_uid, update_uid, is_delete)
                VALUES ('历史用户', 1, '13800138000', 'history@example.com', NOW(), NOW(), 0, 0, 0)
                """);
        assertThrows(SQLException.class, () -> executeUpdate("""
                INSERT INTO `user`
                    (display_name, status, phone, email, update_time, create_time,
                     create_uid, update_uid, is_delete)
                VALUES ('重复用户', 1, '13800138000', 'history@example.com', NOW(), NOW(), 0, 0, 0)
                """));
        executeUpdate("UPDATE `user` SET is_delete = 1 WHERE email = 'history@example.com'");
        executeUpdate("""
                INSERT INTO `user`
                    (display_name, status, phone, email, update_time, create_time,
                     create_uid, update_uid, is_delete)
                VALUES ('重新注册用户', 1, '13800138000', 'history@example.com', NOW(), NOW(), 0, 0, 0)
                """);
        assertEquals(2L, count("SELECT COUNT(*) FROM `user` WHERE email = 'history@example.com'"));

        assertThrows(SQLException.class, () -> executeUpdate("""
                INSERT INTO rbac_role
                    (scope, code, name, built_in, super_role, is_delete, update_time, create_time)
                VALUES ('admin', 'admin:custom-super', '非法超级角色', 0, 1, 0, NOW(), NOW())
                """));
    }

    private Flyway flyway() {
        return Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration/shared")
                .table("flyway_schema_history")
                .cleanDisabled(false)
                .load();
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
