package com.yxx.admin.bootstrap;

import com.yxx.framework.config.security.PasswordConfig;
import com.yxx.security.validation.PasswordPolicyChecker;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * 管理员首次初始化专用配置。
 *
 * <p>该配置只扫描 bootstrap 包和 MyBatis Mapper，不加载正常管理端应用的 Controller、
 * Redis、安全会话及邮件组件。</p>
 */
@SpringBootConfiguration
@Profile("bootstrap")
@EnableAutoConfiguration
@ComponentScan(basePackageClasses = AdminBootstrapRunner.class)
@MapperScan({"com.yxx.admin.mapper", "com.yxx.rbac.mapper"})
@EnableConfigurationProperties(AdminBootstrapProperties.class)
@Import({PasswordConfig.class, PasswordPolicyChecker.class})
public class AdminBootstrapConfiguration {
}
