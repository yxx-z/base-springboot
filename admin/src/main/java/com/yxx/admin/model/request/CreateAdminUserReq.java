package com.yxx.admin.model.request;

import com.yxx.common.validation.TrimmedSize;
import com.yxx.security.validation.Password;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 新增管理员请求。 */
public record CreateAdminUserReq(
        @NotBlank(message = "管理员登录账号不能为空")
        @TrimmedSize(min = 4, max = 50, message = "管理员登录账号规范化后应为4-50位")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "管理员登录账号格式不正确")
        String loginCode,
        @NotBlank(message = "管理员显示名称不能为空")
        @TrimmedSize(min = 2, max = 50, message = "管理员显示名称规范化后应为2-50位")
        String loginName,
        @NotBlank(message = "管理员初始密码不能为空") @Password String password,
        @NotBlank(message = "管理员邮箱不能为空") @Email(message = "管理员邮箱格式不正确")
        @Size(max = 100, message = "管理员邮箱不能超过100位") String email,
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "管理员手机号格式不正确") String linkPhone,
        @NotNull(message = "管理员角色集合不能为空")
        List<@NotNull(message = "角色主键不能为空") @Positive(message = "角色主键必须为正数") Integer> roleIds) {
}
