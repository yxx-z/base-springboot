package com.yxx.admin;

import com.yxx.admin.bootstrap.AdminBootstrapConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication(scanBasePackages = {
        "com.yxx.admin", "com.yxx.common", "com.yxx.security", "com.yxx.framework",
        "com.yxx.rbac"})
@MapperScan({"com.yxx.admin.mapper", "com.yxx.rbac.mapper"})
public class AdminApplication {

    public static void main(String[] args) {
        Class<?> source = isBootstrapMode(args)
                ? AdminBootstrapConfiguration.class
                : AdminApplication.class;
        SpringApplication.run(source, args);
    }

    /**
     * 判断是否启动一次性管理员初始化上下文。
     *
     * <p>bootstrap 模式使用独立、最小化的 Spring 容器，不加载 Web、Redis、邮件和 Sa-Token，
     * 避免首次初始化被无关基础设施阻塞。</p>
     */
    static boolean isBootstrapMode(String[] args) {
        String argumentProfiles = Arrays.stream(args)
                .filter(argument -> argument.startsWith("--spring.profiles.active="))
                .map(argument -> argument.substring(argument.indexOf('=') + 1))
                .findFirst()
                .orElse(null);
        if (argumentProfiles != null) {
            return Arrays.stream(argumentProfiles.split(","))
                .map(String::trim)
                .anyMatch("bootstrap"::equals);
        }
        String environmentProfile = System.getenv("SPRING_PROFILES_ACTIVE");
        return environmentProfile != null
                && Arrays.stream(environmentProfile.split(","))
                .map(String::trim)
                .anyMatch("bootstrap"::equals);
    }
}
