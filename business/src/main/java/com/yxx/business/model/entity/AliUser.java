package com.yxx.business.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

/**
 * 支付宝小程序用户
 *
 * @author yxx
 * @classname AliUser
 * @since 2023-09-14 11:17
 */
@Data
public class AliUser extends BaseEntity{
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 支付宝小程序用户唯一标识
     */
    @NotBlank(message = "支付宝小程序用户唯一标识不能为空")
    private String userId;

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

}
