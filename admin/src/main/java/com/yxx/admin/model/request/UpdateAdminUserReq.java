package com.yxx.admin.model.request;

import com.yxx.common.validation.TrimmedSize;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 修改管理员公开资料请求；稳定登录账号不允许在普通编辑接口中修改。 */
public record UpdateAdminUserReq(
        @NotBlank(message = "管理员显示名称不能为空")
        @TrimmedSize(min = 2, max = 50, message = "管理员显示名称规范化后应为2-50位")
        String loginName,
        @NotBlank(message = "管理员邮箱不能为空") @Email(message = "管理员邮箱格式不正确")
        @Size(max = 100, message = "管理员邮箱不能超过100位") String email,
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "管理员手机号格式不正确") String linkPhone) {
}
