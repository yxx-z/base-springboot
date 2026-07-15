package com.yxx.common.enums.business;

import lombok.Getter;

/**
 * @program: baseProject
 * @description: 角色枚举
 * @author: yxx
 * @date: 2022-02-16 22:11
 */
@Getter
public enum RoleEnum {
    /**
     * 角色
     */
    USER("user", "用户"),
    ADMIN("admin", "管理员"),
    SUPER_ADMIN("super-admin", "超级管理员"),
    ;

    private final String code;
    private final String message;

    RoleEnum(String code, String message){
        this.code = code;
        this.message = message;
    }
}
