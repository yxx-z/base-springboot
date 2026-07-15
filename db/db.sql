SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 用户端账号与认证身份
-- ============================================================
DROP TABLE IF EXISTS `user_identity`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '系统用户主键',
  `display_name` varchar(50) NOT NULL COMMENT '用户显示名称',
  `avatar` varchar(500) DEFAULT NULL COMMENT '用户头像地址',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '账号状态：1-正常，0-停用',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系手机',
  `email` varchar(100) DEFAULT NULL COMMENT '联系邮箱',
  `ip_home_place` varchar(50) DEFAULT NULL COMMENT '最近一次登录IP归属地',
  `agent` varchar(500) DEFAULT NULL COMMENT '最近一次登录设备信息',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_uid` bigint NOT NULL DEFAULT 0 COMMENT '创建人；0表示系统行为',
  `update_uid` bigint NOT NULL DEFAULT 0 COMMENT '修改人；0表示系统行为',
  `is_delete` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_email` (`email`),
  KEY `idx_user_phone` (`phone`),
  KEY `idx_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户端-系统用户主体表';

CREATE TABLE `user_identity` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '登录身份主键',
  `user_id` bigint NOT NULL COMMENT '关联的系统用户主键',
  `identity_type` varchar(30) NOT NULL COMMENT '身份类型：password、alipay，后续可扩展wechat、phone、email',
  `identifier` varchar(191) NOT NULL COMMENT '身份唯一标识：登录账号或第三方平台用户编号',
  `credential` varchar(255) DEFAULT NULL COMMENT '认证凭证；密码身份保存BCrypt，第三方身份为空',
  `verified` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已通过可信渠道验证：1-是，0-否',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '身份状态：1-启用，0-停用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_identity_type_identifier` (`identity_type`, `identifier`),
  KEY `idx_user_identity_user_id` (`user_id`),
  CONSTRAINT `fk_user_identity_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户端-多登录身份绑定表';

-- ============================================================
-- 用户端 RBAC：菜单与后端权限分离
-- ============================================================
DROP TABLE IF EXISTS `role_permission`;
DROP TABLE IF EXISTS `role_menu`;
DROP TABLE IF EXISTS `user_role`;
DROP TABLE IF EXISTS `permission`;
DROP TABLE IF EXISTS `menu`;
DROP TABLE IF EXISTS `role`;

CREATE TABLE `role` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '用户端角色主键',
  `code` varchar(100) NOT NULL COMMENT '角色编码，采用冒号分层格式',
  `name` varchar(50) NOT NULL COMMENT '角色名称',
  `remark` varchar(255) DEFAULT NULL COMMENT '角色说明',
  `is_delete` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户端-角色表';

CREATE TABLE `permission` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '用户端权限主键',
  `code` varchar(150) NOT NULL COMMENT '后端权限编码，例如user:profile:read',
  `name` varchar(100) NOT NULL COMMENT '权限名称',
  `resource_type` varchar(30) NOT NULL DEFAULT 'api' COMMENT '资源类型：api、operation、data',
  `description` varchar(255) DEFAULT NULL COMMENT '权限说明',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '权限状态：1-启用，0-停用',
  `is_delete` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`code`),
  KEY `idx_permission_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户端-后端权限资源表';

CREATE TABLE `menu` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '用户端菜单主键',
  `parent_id` int DEFAULT NULL COMMENT '父菜单主键；顶级菜单为空',
  `menu_code` varchar(100) NOT NULL COMMENT '前端菜单唯一编码',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `path` varchar(255) DEFAULT NULL COMMENT '前端路由路径',
  `component` varchar(255) DEFAULT NULL COMMENT '前端组件标识',
  `icon` varchar(100) DEFAULT NULL COMMENT '菜单图标',
  `sort` int NOT NULL DEFAULT 0 COMMENT '同级菜单排序值，越小越靠前',
  `visible` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否在导航中显示：1-显示，0-隐藏',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '菜单状态：1-启用，0-停用',
  `is_delete` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_menu_code` (`menu_code`),
  KEY `idx_menu_parent_id` (`parent_id`),
  CONSTRAINT `fk_menu_parent` FOREIGN KEY (`parent_id`) REFERENCES `menu` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户端-前端导航菜单表';

CREATE TABLE `user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户角色关联主键',
  `user_id` bigint NOT NULL COMMENT '系统用户主键',
  `role_id` int NOT NULL COMMENT '用户端角色主键',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_user_role_role_id` (`role_id`),
  CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户端-用户角色关联表；关联数据采用物理删除';

CREATE TABLE `role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色权限关联主键',
  `role_id` int NOT NULL COMMENT '用户端角色主键',
  `permission_id` int NOT NULL COMMENT '用户端权限主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
  KEY `idx_role_permission_permission_id` (`permission_id`),
  CONSTRAINT `fk_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`),
  CONSTRAINT `fk_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户端-角色后端权限关联表；关联数据采用物理删除';

CREATE TABLE `role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色菜单关联主键',
  `role_id` int NOT NULL COMMENT '用户端角色主键',
  `menu_id` int NOT NULL COMMENT '用户端菜单主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
  KEY `idx_role_menu_menu_id` (`menu_id`),
  CONSTRAINT `fk_role_menu_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`),
  CONSTRAINT `fk_role_menu_menu` FOREIGN KEY (`menu_id`) REFERENCES `menu` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户端-角色前端菜单关联表；关联数据采用物理删除';

-- ============================================================
-- 管理端账号与独立 RBAC
-- ============================================================
DROP TABLE IF EXISTS `admin_role_permission`;
DROP TABLE IF EXISTS `admin_role_menu`;
DROP TABLE IF EXISTS `admin_user_role`;
DROP TABLE IF EXISTS `admin_permission`;
DROP TABLE IF EXISTS `admin_menu`;
DROP TABLE IF EXISTS `admin_role`;
DROP TABLE IF EXISTS `admin_user`;

CREATE TABLE `admin_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '管理员主键',
  `login_code` varchar(50) NOT NULL COMMENT '管理员登录账号',
  `login_name` varchar(50) NOT NULL COMMENT '管理员显示名称',
  `password` varchar(255) NOT NULL COMMENT 'BCrypt密码摘要',
  `link_phone` varchar(20) DEFAULT NULL COMMENT '联系手机',
  `email` varchar(100) NOT NULL COMMENT '联系邮箱',
  `ip_home_place` varchar(50) DEFAULT NULL COMMENT '最近一次登录IP归属地',
  `agent` varchar(500) DEFAULT NULL COMMENT '最近一次登录设备信息',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_uid` bigint NOT NULL DEFAULT 0 COMMENT '创建人；0表示系统行为',
  `update_uid` bigint NOT NULL DEFAULT 0 COMMENT '修改人；0表示系统行为',
  `is_delete` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_user_login_code` (`login_code`),
  UNIQUE KEY `uk_admin_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='管理端-管理员表';

CREATE TABLE `admin_role` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '管理端角色主键',
  `code` varchar(100) NOT NULL COMMENT '角色编码，采用冒号分层格式',
  `name` varchar(50) NOT NULL COMMENT '角色名称',
  `remark` varchar(255) DEFAULT NULL COMMENT '角色说明',
  `is_delete` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='管理端-角色表';

CREATE TABLE `admin_permission` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '管理端权限主键',
  `code` varchar(150) NOT NULL COMMENT '后端权限编码，例如admin:user:read',
  `name` varchar(100) NOT NULL COMMENT '权限名称',
  `resource_type` varchar(30) NOT NULL DEFAULT 'api' COMMENT '资源类型：api、operation、data',
  `description` varchar(255) DEFAULT NULL COMMENT '权限说明',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '权限状态：1-启用，0-停用',
  `is_delete` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_permission_code` (`code`),
  KEY `idx_admin_permission_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='管理端-后端权限资源表';

CREATE TABLE `admin_menu` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '管理端菜单主键',
  `parent_id` int DEFAULT NULL COMMENT '父菜单主键；顶级菜单为空',
  `menu_code` varchar(100) NOT NULL COMMENT '前端菜单唯一编码',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `path` varchar(255) DEFAULT NULL COMMENT '前端路由路径',
  `component` varchar(255) DEFAULT NULL COMMENT '前端组件标识',
  `icon` varchar(100) DEFAULT NULL COMMENT '菜单图标',
  `sort` int NOT NULL DEFAULT 0 COMMENT '同级菜单排序值，越小越靠前',
  `visible` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否在导航中显示：1-显示，0-隐藏',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '菜单状态：1-启用，0-停用',
  `is_delete` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_menu_code` (`menu_code`),
  KEY `idx_admin_menu_parent_id` (`parent_id`),
  CONSTRAINT `fk_admin_menu_parent` FOREIGN KEY (`parent_id`) REFERENCES `admin_menu` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='管理端-前端导航菜单表';

CREATE TABLE `admin_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '管理员角色关联主键',
  `user_id` bigint NOT NULL COMMENT '管理员主键',
  `role_id` int NOT NULL COMMENT '管理端角色主键',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_user_role` (`user_id`, `role_id`),
  KEY `idx_admin_user_role_role_id` (`role_id`),
  CONSTRAINT `fk_admin_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `admin_user` (`id`),
  CONSTRAINT `fk_admin_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `admin_role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='管理端-管理员角色关联表；关联数据采用物理删除';

CREATE TABLE `admin_role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '管理端角色权限关联主键',
  `role_id` int NOT NULL COMMENT '管理端角色主键',
  `permission_id` int NOT NULL COMMENT '管理端权限主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_role_permission` (`role_id`, `permission_id`),
  KEY `idx_admin_role_permission_permission_id` (`permission_id`),
  CONSTRAINT `fk_admin_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `admin_role` (`id`),
  CONSTRAINT `fk_admin_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `admin_permission` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='管理端-角色后端权限关联表；关联数据采用物理删除';

CREATE TABLE `admin_role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '管理端角色菜单关联主键',
  `role_id` int NOT NULL COMMENT '管理端角色主键',
  `menu_id` int NOT NULL COMMENT '管理端菜单主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_role_menu` (`role_id`, `menu_id`),
  KEY `idx_admin_role_menu_menu_id` (`menu_id`),
  CONSTRAINT `fk_admin_role_menu_role` FOREIGN KEY (`role_id`) REFERENCES `admin_role` (`id`),
  CONSTRAINT `fk_admin_role_menu_menu` FOREIGN KEY (`menu_id`) REFERENCES `admin_menu` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='管理端-角色前端菜单关联表；关联数据采用物理删除';

-- ============================================================
-- 操作审计日志
-- ============================================================
DROP TABLE IF EXISTS `operate_admin_log`;
DROP TABLE IF EXISTS `operate_log`;

CREATE TABLE `operate_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户端审计日志主键',
  `user_id` bigint DEFAULT NULL COMMENT '操作用户主键；匿名操作为空',
  `type` tinyint NOT NULL COMMENT '执行结果：1-成功，2-失败',
  `module` varchar(100) NOT NULL COMMENT '业务模块',
  `title` varchar(255) NOT NULL COMMENT '结构化操作名称',
  `resource` varchar(150) DEFAULT NULL COMMENT '被操作的业务资源类型或说明',
  `ip` varchar(50) DEFAULT NULL COMMENT '客户端IP',
  `ip_home_place` varchar(50) DEFAULT NULL COMMENT '客户端IP归属地',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '客户端User-Agent',
  `request_uri` varchar(500) DEFAULT NULL COMMENT '请求URI',
  `method` varchar(20) DEFAULT NULL COMMENT 'HTTP请求方法',
  `params` text COMMENT '经过脱敏和截断的请求参数',
  `trace_id` varchar(200) DEFAULT NULL COMMENT '请求链路标识',
  `span_id` varchar(100) DEFAULT NULL COMMENT '预留的调用跨度标识',
  `time` bigint DEFAULT NULL COMMENT '接口执行耗时，单位毫秒',
  `exception` text COMMENT '经过脱敏的异常摘要',
  `create_uid` bigint DEFAULT NULL COMMENT '日志创建人；通常与user_id一致',
  `create_time` datetime NOT NULL COMMENT '日志创建时间',
  `is_delete` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_operate_log_user_time` (`user_id`, `create_time`),
  KEY `idx_operate_log_type_time` (`type`, `create_time`),
  KEY `idx_operate_log_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户端-操作审计日志表';

CREATE TABLE `operate_admin_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '管理端审计日志主键',
  `user_id` bigint DEFAULT NULL COMMENT '操作管理员主键；匿名操作为空',
  `type` tinyint NOT NULL COMMENT '执行结果：1-成功，2-失败',
  `module` varchar(100) NOT NULL COMMENT '业务模块',
  `title` varchar(255) NOT NULL COMMENT '结构化操作名称',
  `resource` varchar(150) DEFAULT NULL COMMENT '被操作的业务资源类型或说明',
  `ip` varchar(50) DEFAULT NULL COMMENT '客户端IP',
  `ip_home_place` varchar(50) DEFAULT NULL COMMENT '客户端IP归属地',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '客户端User-Agent',
  `request_uri` varchar(500) DEFAULT NULL COMMENT '请求URI',
  `method` varchar(20) DEFAULT NULL COMMENT 'HTTP请求方法',
  `params` text COMMENT '经过脱敏和截断的请求参数',
  `trace_id` varchar(200) DEFAULT NULL COMMENT '请求链路标识',
  `span_id` varchar(100) DEFAULT NULL COMMENT '预留的调用跨度标识',
  `time` bigint DEFAULT NULL COMMENT '接口执行耗时，单位毫秒',
  `exception` text COMMENT '经过脱敏的异常摘要',
  `create_uid` bigint DEFAULT NULL COMMENT '日志创建人；通常与user_id一致',
  `create_time` datetime NOT NULL COMMENT '日志创建时间',
  `is_delete` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_operate_admin_log_user_time` (`user_id`, `create_time`),
  KEY `idx_operate_admin_log_type_time` (`type`, `create_time`),
  KEY `idx_operate_admin_log_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='管理端-操作审计日志表';

-- ============================================================
-- 基础角色、权限和菜单初始化
-- ============================================================
INSERT INTO `role` (`code`, `name`, `remark`, `is_delete`, `update_time`, `create_time`) VALUES
  ('user:member', '普通用户', '用户首次注册或首次第三方登录时分配的默认角色', 0, NOW(), NOW()),
  ('user:administrator', '用户端管理员', '负责用户端管理能力的角色', 0, NOW(), NOW());

INSERT INTO `permission` (`code`, `name`, `resource_type`, `description`, `status`, `is_delete`) VALUES
  ('audit:log:read', '查看用户端审计日志', 'api', '允许查询用户端操作审计日志', 1, 0);

INSERT INTO `menu` (`parent_id`, `menu_code`, `menu_name`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `is_delete`) VALUES
  (NULL, 'audit-log', '审计日志', '/audit/log', 'audit/LogIndex', 'document', 100, 1, 1, 0);

INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `role` r JOIN `permission` p
WHERE r.code = 'user:administrator' AND p.code = 'audit:log:read';

INSERT INTO `role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id FROM `role` r JOIN `menu` m
WHERE r.code = 'user:administrator' AND m.menu_code = 'audit-log';

INSERT INTO `admin_role` (`code`, `name`, `remark`, `is_delete`, `update_time`, `create_time`) VALUES
  ('admin:administrator', '管理员', '管理端普通管理员角色', 0, NOW(), NOW()),
  ('admin:super-admin', '超级管理员', '管理端最高权限角色', 0, NOW(), NOW());

INSERT INTO `admin_permission` (`code`, `name`, `resource_type`, `description`, `status`, `is_delete`) VALUES
  ('audit:log:read', '查看管理端审计日志', 'api', '允许查询管理端操作审计日志', 1, 0);

INSERT INTO `admin_menu` (`parent_id`, `menu_code`, `menu_name`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `is_delete`) VALUES
  (NULL, 'audit-log', '审计日志', '/audit/log', 'audit/LogIndex', 'document', 100, 1, 1, 0);

INSERT INTO `admin_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `admin_role` r JOIN `admin_permission` p
WHERE r.code IN ('admin:administrator', 'admin:super-admin') AND p.code = 'audit:log:read';

INSERT INTO `admin_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id FROM `admin_role` r JOIN `admin_menu` m
WHERE r.code IN ('admin:administrator', 'admin:super-admin') AND m.menu_code = 'audit-log';

SET FOREIGN_KEY_CHECKS = 1;
