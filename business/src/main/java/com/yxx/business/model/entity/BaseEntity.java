package com.yxx.business.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实体类基础数据
 *
 * @author yxx
 * @classname BaseEntity
 * @since 2023-06-29 21:44
 */
@Data
public class BaseEntity {

    /**
     * 是否删除:0-未删除；1-已删除
     */
    @TableLogic
    private Boolean isDelete;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createUid;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUid;
}
