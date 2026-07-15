package com.yxx.common.core.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录用户身份权限
 *
 * @author yxx
 * @since 2022/4/13 14:23
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class LoginUser {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户token
     */
    private String token;

    /**
     * 用户账号
     */
    private String loginCode;

    /**
     * 登录名称
     */
    private String loginName;

    /**
     * 学号
     */
    private String studentNumber;

    /**
     * 联系手机
     */
    private String linkPhone;

    /**
     * ip归属地
     */
    private String ipHomePlace;

    /**
     * 登录设备
     */
    private String agent;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 菜单权限
     */
    private List<String> menuPermission;

    /**
     * 按钮权限
     */
    private List<String> buttonPermission;

    /**
     * 角色权限
     */
    private List<String> rolePermission;

    /**
     * 登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime loginTime;
}
