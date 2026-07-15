package com.yxx.admin;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
