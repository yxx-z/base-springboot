package com.yxx.business.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;


/**
 * 用户登录身份。
 *
 * <p>一名系统用户可以绑定密码、支付宝、微信、手机号等多个身份。外部平台用户编号和密码
 * 凭证只保存在身份表中，业务表统一使用 {@code userId} 关联系统用户。</p>
 */
@Data
public class UserIdentity {

    /** 身份记录主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的系统用户主键。 */
    private Long userId;

    /** 身份类型，例如 password、alipay。 */
    private String identityType;

    /** 身份唯一标识，例如登录账号或支付宝用户编号。 */
    private String identifier;

    /** 认证凭证；密码身份保存 BCrypt，第三方身份为空。 */
    private String credential;

    /** 身份是否已经通过可信渠道验证。 */
    private Boolean verified;

    /** 身份状态：1-启用，0-停用。停用身份不会参与认证。 */
    private Boolean status;
}
