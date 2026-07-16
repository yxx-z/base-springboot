package com.yxx.business.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.LocalDateTime;


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

    /** 最后修改时间，由 MyBatis-Plus 审计字段处理器统一维护。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建时间，由 MyBatis-Plus 审计字段处理器统一维护。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除标记。
     *
     * <p>注销用户时身份记录不会物理删除，支付宝用户编号、登录账号等历史标识继续保留，
     * 供后续反复注销注册风控使用；数据库生成列只约束未删除身份的唯一性。</p>
     */
    @TableLogic
    private Boolean isDelete;
}
