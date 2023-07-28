package com.yxx.common.core.model;

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
 * @since  2022/4/13 14:23
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
     * 联系手机
     */
    private String linkPhone;

    /**
     * ip归属地
     */
    private String ipHomePlace;

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
    private LocalDateTime loginTime;
}
