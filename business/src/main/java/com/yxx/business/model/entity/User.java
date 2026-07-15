package com.yxx.business.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

/**
 * @author yxx
 * @since 2022-11-12 13:38
 */
@Data
public class User extends BaseEntity implements Serializable{
    /**
     * 用户id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户显示名称。 */
    private String displayName;

    /** 用户头像地址。 */
    private String avatar;

    /** 账号状态：1-正常，0-停用。 */
    private Boolean status;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * ip归属地
     */
    private String ipHomePlace;

    /**
     * 登录设备
     */
    private String agent;
}
