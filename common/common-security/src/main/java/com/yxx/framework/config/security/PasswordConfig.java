package com.yxx.framework.config.security;

import com.yxx.security.properties.PasswordPolicyProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码安全配置。
 *
 * <p>基础框架统一提供密码编码器，禁止业务代码自行选择 MD5、SHA 等不适合保存密码的算法，
 * 从而确保注册、登录、修改密码和重置密码使用完全一致的安全策略。</p>
 */
@Configuration
@EnableConfigurationProperties(PasswordPolicyProperties.class)
public class PasswordConfig {

    /**
     * 创建密码编码器。
     *
     * <p>BCrypt 会为每个密码生成独立随机盐，并将计算成本写入密文，便于未来逐步提高强度。</p>
     *
     * @return 统一密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
