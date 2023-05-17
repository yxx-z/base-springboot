package com.yxx.business.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yxx.common.core.page.BasePageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * @author yxx
 * @since 2022/8/1 17:39
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperateLogReq extends BasePageRequest implements Serializable {

    /**
     * 用户名
     */
    private String loginCode;

    /**
     * 用户名称
     */
    private String loginName;

    /**
     * 操作内容
     */
    private String title;


    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate endTime;
}
