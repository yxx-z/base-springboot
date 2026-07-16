package com.yxx.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端使用的业务用户管理投影。
 *
 * <p>admin 与 business 共用数据库，但管理端不依赖 business 可执行模块。该模型只映射
 * 管理后台真正需要的用户字段，避免复制业务端认证身份和密码处理逻辑。</p>
 */
@Data
@TableName("user")
public class ManagedBusinessUser {

    @TableId
    private Long id;

    private String displayName;

    private String avatar;

    private Boolean status;

    private String phone;

    private String email;

    private String ipHomePlace;

    private String agent;

    private LocalDateTime updateTime;

    private LocalDateTime createTime;

    @TableLogic
    private Boolean isDelete;
}
