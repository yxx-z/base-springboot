package com.yxx.business.model.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @author yxx
 * @since 2022/8/1 17:43
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class OperateLogResp {
    private static final long serialVersionUID = -6160374035388312821L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 操作模块
     */
    private String module;

    /**
     * 日志标题
     */
    private String title;

    /**
     * 操作IP
     */
    private String ip;

    /**
     * 操作用户
     */
    private String loginCode;

    /**
     * 用户名称
     */
    private String loginName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
