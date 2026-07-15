package com.yxx.admin;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 管理端应用最小结构测试。
 *
 * <p>该测试不访问真实数据库、缓存和邮件服务，确保普通单元测试可以在任意开发机和 CI
 * 环境中重复执行。</p>
 */
class AdminApplicationTests {

    @Test
    void shouldExposeApplicationEntryPoint() {
        assertNotNull(AdminApplication.class);
    }

    @Test
    void shouldParseAllYamlConfigurationsWithoutDuplicateKeys() throws IOException {
        assertValidYaml("application.yml");
        assertValidYaml("application-dev.yml");
        assertValidYaml("application-prod.yml");
        assertValidYaml("application-bootstrap.yml");
    }

    @Test
    void shouldSelectBootstrapContextOnlyForBootstrapProfile() {
        assertTrue(AdminApplication.isBootstrapMode(
                new String[]{"--spring.profiles.active=bootstrap"}));
        assertFalse(AdminApplication.isBootstrapMode(
                new String[]{"--spring.profiles.active=prod"}));
    }

    @Test
    void shouldProvideNonDestructiveAdminMigration() throws IOException {
        assertMigrationDoesNotContainDrop("db/migration/admin/V1__init_admin_schema.sql");
    }

    private void assertValidYaml(String resourceName) throws IOException {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(options));
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(inputStream, "找不到配置文件：" + resourceName);
            yaml.load(inputStream);
        }
    }

    private void assertMigrationDoesNotContainDrop(String resourceName) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(inputStream, "找不到迁移文件：" + resourceName);
            String sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(sql.toUpperCase().contains("DROP TABLE"), "Flyway 基线迁移禁止删除表");
        }
    }
}
