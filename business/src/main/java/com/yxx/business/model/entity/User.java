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

    /**
     * 登录账号
     */
    private String loginCode;

    /**
     * 登录名
     */
    private String loginName;

    /**
     * 密码
     */
    private String password;

    /**
     * 手机号
     */
    private String linkPhone;

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
