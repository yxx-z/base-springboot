/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80026
 Source Host           : 127.0.0.1:3306
 Source Schema         : base_springboot

 Target Server Type    : MySQL
 Target Server Version : 80026
 File Encoding         : 65001

 Date: 15/05/2023 17:51:41
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for operate_log
-- ----------------------------
DROP TABLE IF EXISTS `operate_log`;
CREATE TABLE `operate_log`
(
    `id`          bigint                                                        NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `user_id`     bigint                                                                 DEFAULT NULL COMMENT '用户id',
    `type`        char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci      NOT NULL COMMENT '类型：1-正常日志 2-异常日志',
    `module`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '操作模块',
    `title`       varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '日志标题',
    `ip`          varchar(50) COLLATE utf8mb4_general_ci                                 DEFAULT NULL COMMENT '操作IP',
    `user_agent`  varchar(500) COLLATE utf8mb4_general_ci                                DEFAULT NULL COMMENT '用户代理',
    `request_uri` varchar(500) COLLATE utf8mb4_general_ci                                DEFAULT NULL COMMENT '请求URI',
    `method`      varchar(20) COLLATE utf8mb4_general_ci                                 DEFAULT NULL COMMENT '操作方式',
    `params`      text COLLATE utf8mb4_general_ci COMMENT '操作提交的数据',
    `trace_id`    bigint                                                                 DEFAULT NULL COMMENT 'tLog中的traceId',
    `span_id`     int                                                                    DEFAULT NULL COMMENT 'tLog中的spanId',
    `time`        bigint                                                                 DEFAULT NULL COMMENT '执行时间',
    `exception`   varchar(500) COLLATE utf8mb4_general_ci                                DEFAULT NULL COMMENT '异常信息',
    `create_uid`  bigint                                                                 DEFAULT NULL COMMENT '创建人',
    `create_time` datetime                                                      NOT NULL COMMENT '创建时间',
    `is_delete`   char(1) COLLATE utf8mb4_general_ci                            NOT NULL DEFAULT '0' COMMENT '是否删除 0-否 1-是',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 15
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='操作日志表';

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`
(
    `id`          bigint                                                        NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `login_code`  varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '登录账号',
    `login_name`  varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '用户名称',
    `password`    varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
    `link_phone`  varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '联系号码',
    `update_time` datetime                                                      NOT NULL COMMENT '修改时间',
    `create_time` datetime                                                      NOT NULL COMMENT '创建时间',
    `create_uid`  bigint                                                        NOT NULL COMMENT '创建人',
    `update_uid`  bigint                                                        NOT NULL COMMENT '修改人',
    `is_delete`   char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci      NOT NULL DEFAULT '0' COMMENT '是否删除:0-未删除；1-已删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='用户表';

SET FOREIGN_KEY_CHECKS = 1;
