package com.yxx.business;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 业务应用最小结构测试。
 *
 * <p>测试阶段不连接开发人员本机的 MySQL、Redis 和邮件服务器。完整基础设施联调应放入
 * 独立集成测试阶段，并通过 Testcontainers 或 CI 服务容器提供依赖。</p>
 */
class BusinessApplicationTests {

    @Test
    void shouldExposeApplicationEntryPoint() {
        assertNotNull(BusinessApplication.class);
    }

    @Test
    void shouldParseAllYamlConfigurationsWithoutDuplicateKeys() throws IOException {
        assertValidYaml("application.yml");
        assertValidYaml("application-dev.yml");
        assertValidYaml("application-prod.yml");
    }

    @Test
    void shouldProvideNonDestructiveBusinessMigration() throws IOException {
        assertMigrationDoesNotContainDrop("db/migration/shared/V1__init_shared_schema.sql");
        assertMigrationDoesNotContainDrop("db/migration/shared/V2__add_audit_actor_snapshot.sql");
    }

    private void assertMigrationDoesNotContainDrop(String resourceName) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(inputStream, "找不到迁移文件：" + resourceName);
            String sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(sql.toUpperCase().contains("DROP TABLE"), "Flyway 基线迁移禁止删除表");
        }
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
}
