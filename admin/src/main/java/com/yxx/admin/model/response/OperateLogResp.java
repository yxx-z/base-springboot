package com.yxx.admin.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
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
     * 类型：1:成功；2:失败
     */
    private Integer type;

    /**
     * 异常信息
     */
    private String exception;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
