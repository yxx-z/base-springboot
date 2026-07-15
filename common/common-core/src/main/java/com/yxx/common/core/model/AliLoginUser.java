package com.yxx.common.core.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

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
public class AliLoginUser {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 支付宝小程序用户唯一标识
     */
    private String userId;

    /**
     * 用户token
     */
    private String token;
    /**
     * 头像
     */
    private String avatar;

    /**
     * 国家
     */
    private String countryCode;

    /**
     * 范围
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    private String nickName;

    /**
     * 性别
     */
    private String gender;

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
