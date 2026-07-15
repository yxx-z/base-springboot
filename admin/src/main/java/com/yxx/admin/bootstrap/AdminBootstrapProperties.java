package com.yxx.admin.bootstrap;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 管理员首次初始化参数。 */
@Data
@ConfigurationProperties(prefix = "bootstrap.admin")
public class AdminBootstrapProperties {

    /** 初始管理员登录账号。 */
    private String loginCode;

    /** 初始管理员显示名称。 */
    private String loginName;

    /** 初始管理员联系邮箱。 */
    private String email;

    /** 初始管理员临时密码。 */
    private String password;
}
