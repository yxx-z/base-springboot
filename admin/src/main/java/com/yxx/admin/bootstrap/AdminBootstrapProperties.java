package com.yxx.admin.bootstrap;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import com.yxx.common.validation.TrimmedSize;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 管理员首次初始化参数。 */
@Data
@Validated
@ConfigurationProperties(prefix = "bootstrap.admin")
public class AdminBootstrapProperties {

    /** 初始管理员登录账号。 */
    @NotBlank(message = "初始管理员登录账号不能为空")
    @TrimmedSize(min = 4, max = 50, message = "初始管理员登录账号规范化后应为4-50位")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "初始管理员登录账号格式不正确")
    private String loginCode;

    /** 初始管理员显示名称。 */
    @NotBlank(message = "初始管理员显示名称不能为空")
    @TrimmedSize(min = 2, max = 50, message = "初始管理员显示名称规范化后应为2-50位")
    private String loginName;

    /** 初始管理员联系邮箱。 */
    @NotBlank(message = "初始管理员邮箱不能为空")
    @Email(message = "初始管理员邮箱格式不正确")
    @Size(max = 100, message = "初始管理员邮箱不能超过100位")
    private String email;

    /** 初始管理员临时密码。 */
    @NotBlank(message = "初始管理员临时密码不能为空")
    private String password;
}
