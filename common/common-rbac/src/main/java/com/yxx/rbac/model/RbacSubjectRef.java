package com.yxx.rbac.model;

/**
 * 持有角色的授权主体引用。
 *
 * @param subjectType 主体类型编码
 * @param subjectId   主体稳定主键
 */
public record RbacSubjectRef(String subjectType, Long subjectId) {
}
