-- admin 是管理面、business 是业务面；两者共用一个 Schema 和一条 Flyway 版本链。
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '业务用户主键',
  `display_name` varchar(50) NOT NULL COMMENT '用户显示名称',
  `avatar` varchar(500) DEFAULT NULL COMMENT '用户头像地址',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '账号状态：1-正常，0-停用',
  `phone` varchar(20) DEFAULT NULL COMMENT '中国大陆联系手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '联系邮箱',
  `ip_home_place` varchar(50) DEFAULT NULL COMMENT '最近一次登录IP归属地',
  `agent` varchar(500) DEFAULT NULL COMMENT '最近一次登录设备信息',
  `update_time` datetime NOT NULL COMMENT '最后修改时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_uid` bigint NOT NULL DEFAULT 0 COMMENT '创建人；0表示系统行为',
  `update_uid` bigint NOT NULL DEFAULT 0 COMMENT '修改人；0表示系统行为',
  `is_delete` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `active_email` varchar(100)
    GENERATED ALWAYS AS (CASE WHEN `is_delete` = 0 THEN `email` ELSE NULL END) STORED
    COMMENT '仅未删除用户参与唯一约束的邮箱；软删记录为NULL',
  `active_phone` varchar(20)
    GENERATED ALWAYS AS (CASE WHEN `is_delete` = 0 THEN `phone` ELSE NULL END) STORED
    COMMENT '仅未删除用户参与唯一约束的手机号；软删记录为NULL',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_active_email` (`active_email`),
  UNIQUE KEY `uk_user_active_phone` (`active_phone`),
  KEY `idx_user_history_email` (`email`),
  KEY `idx_user_history_phone` (`phone`),
  KEY `idx_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='业务端-用户主体表';

CREATE TABLE `user_identity` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '登录身份主键',
  `user_id` bigint NOT NULL COMMENT '关联的业务用户主键',
  `identity_type` varchar(30) NOT NULL COMMENT '身份类型：password、alipay；可扩展wechat、phone、email',
  `identifier` varchar(191) NOT NULL COMMENT '身份唯一标识：登录账号或第三方平台用户编号',
  `credential` varchar(255) DEFAULT NULL COMMENT '认证凭证；密码身份保存BCrypt摘要，第三方身份为空',
  `verified` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已通过可信渠道验证：1-是，0-否',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '身份状态：1-启用，0-停用',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `active_identifier` varchar(191)
    GENERATED ALWAYS AS (CASE WHEN `is_delete` = 0 THEN `identifier` ELSE NULL END) STORED
    COMMENT '仅未删除身份参与唯一约束的身份标识；软删记录为NULL',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_identity_active_identifier` (`identity_type`, `active_identifier`),
  KEY `idx_user_identity_history_identifier` (`identity_type`, `identifier`),
  KEY `idx_user_identity_user_delete` (`user_id`, `is_delete`),
  CONSTRAINT `fk_user_identity_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='业务端-多登录身份绑定表';

CREATE TABLE `admin_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '管理员主键',
  `login_code` varchar(50) NOT NULL COMMENT '管理员登录账号',
  `login_name` varchar(50) NOT NULL COMMENT '管理员显示名称',
  `password` varchar(255) NOT NULL COMMENT 'BCrypt密码摘要',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '管理员状态：1-正常，0-停用',
  `link_phone` varchar(20) DEFAULT NULL COMMENT '中国大陆联系手机号',
  `email` varchar(100) NOT NULL COMMENT '联系邮箱',
  `ip_home_place` varchar(50) DEFAULT NULL COMMENT '最近一次登录IP归属地',
  `agent` varchar(500) DEFAULT NULL COMMENT '最近一次登录设备信息',
  `update_time` datetime NOT NULL COMMENT '最后修改时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_uid` bigint NOT NULL DEFAULT 0 COMMENT '创建人；0表示系统行为',
  `update_uid` bigint NOT NULL DEFAULT 0 COMMENT '修改人；0表示系统行为',
  `is_delete` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `active_login_code` varchar(50)
    GENERATED ALWAYS AS (CASE WHEN `is_delete` = 0 THEN `login_code` ELSE NULL END) STORED
    COMMENT '仅未删除管理员参与唯一约束的登录账号；软删记录为NULL',
  `active_email` varchar(100)
    GENERATED ALWAYS AS (CASE WHEN `is_delete` = 0 THEN `email` ELSE NULL END) STORED
    COMMENT '仅未删除管理员参与唯一约束的邮箱；软删记录为NULL',
  `active_link_phone` varchar(20)
    GENERATED ALWAYS AS (CASE WHEN `is_delete` = 0 THEN `link_phone` ELSE NULL END) STORED
    COMMENT '仅未删除管理员参与唯一约束的手机号；软删记录为NULL',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_user_active_login_code` (`active_login_code`),
  UNIQUE KEY `uk_admin_user_active_email` (`active_email`),
  UNIQUE KEY `uk_admin_user_active_link_phone` (`active_link_phone`),
  KEY `idx_admin_user_history_login_code` (`login_code`),
  KEY `idx_admin_user_history_email` (`email`),
  KEY `idx_admin_user_history_link_phone` (`link_phone`),
  KEY `idx_admin_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='管理端-管理员账号表';

-- 一套 RBAC 表通过 scope 隔离管理端与业务端资源，而不是复制两套表和服务。
CREATE TABLE `rbac_role` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '统一角色主键',
  `scope` varchar(20) NOT NULL COMMENT '权限域：admin-管理端，business-业务端',
  `code` varchar(100) NOT NULL COMMENT '权限域内唯一的角色编码，采用冒号分层格式',
  `name` varchar(50) NOT NULL COMMENT '角色名称',
  `remark` varchar(255) DEFAULT NULL COMMENT '角色用途说明',
  `built_in` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为不可修改授权关系的内置角色：1-是，0-否',
  `super_role` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为超级角色：1-是，0-否；仅管理端允许配置',
  `is_delete` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `update_time` datetime NOT NULL COMMENT '最后修改时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `active_code` varchar(100)
    GENERATED ALWAYS AS (CASE WHEN `is_delete` = 0 THEN `code` ELSE NULL END) STORED
    COMMENT '仅未删除角色参与唯一约束的角色编码；软删记录为NULL',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rbac_role_scope_active_code` (`scope`, `active_code`),
  UNIQUE KEY `uk_rbac_role_id_scope` (`id`, `scope`),
  KEY `idx_rbac_role_history_code` (`scope`, `code`),
  CONSTRAINT `ck_rbac_role_scope` CHECK (`scope` IN ('admin', 'business')),
  CONSTRAINT `ck_rbac_role_super_invariant` CHECK (
    (`super_role` = 0 AND `code` <> 'admin:super-admin') OR
    (`super_role` = 1 AND `built_in` = 1 AND `scope` = 'admin'
      AND `code` = 'admin:super-admin')
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='统一RBAC-角色表';

CREATE TABLE `rbac_permission` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '统一权限主键',
  `scope` varchar(20) NOT NULL COMMENT '权限域：admin-管理端，business-业务端',
  `code` varchar(150) NOT NULL COMMENT '权限域内唯一的后端权限编码',
  `name` varchar(100) NOT NULL COMMENT '权限名称',
  `resource_type` varchar(30) NOT NULL DEFAULT 'api' COMMENT '资源类型：api、operation、data',
  `description` varchar(255) DEFAULT NULL COMMENT '权限用途说明',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '权限状态：1-启用，0-停用',
  `is_delete` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `active_code` varchar(150)
    GENERATED ALWAYS AS (CASE WHEN `is_delete` = 0 THEN `code` ELSE NULL END) STORED
    COMMENT '仅未删除权限参与唯一约束的权限编码；软删记录为NULL',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rbac_permission_scope_active_code` (`scope`, `active_code`),
  UNIQUE KEY `uk_rbac_permission_id_scope` (`id`, `scope`),
  KEY `idx_rbac_permission_history_code` (`scope`, `code`),
  KEY `idx_rbac_permission_scope_status` (`scope`, `status`),
  CONSTRAINT `ck_rbac_permission_scope` CHECK (`scope` IN ('admin', 'business'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='统一RBAC-后端权限资源表';

CREATE TABLE `rbac_menu` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '统一菜单主键',
  `scope` varchar(20) NOT NULL COMMENT '权限域：admin-管理端，business-业务端',
  `parent_id` int DEFAULT NULL COMMENT '同权限域父菜单主键；顶级菜单为空',
  `menu_code` varchar(100) NOT NULL COMMENT '权限域内唯一的前端菜单编码',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `path` varchar(255) DEFAULT NULL COMMENT '前端路由路径',
  `component` varchar(255) DEFAULT NULL COMMENT '前端组件标识',
  `icon` varchar(100) DEFAULT NULL COMMENT '菜单图标',
  `sort` int NOT NULL DEFAULT 0 COMMENT '同级菜单排序值，数值越小越靠前',
  `visible` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否在导航中显示：1-显示，0-隐藏',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '菜单状态：1-启用，0-停用',
  `is_delete` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `active_menu_code` varchar(100)
    GENERATED ALWAYS AS (CASE WHEN `is_delete` = 0 THEN `menu_code` ELSE NULL END) STORED
    COMMENT '仅未删除菜单参与唯一约束的菜单编码；软删记录为NULL',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rbac_menu_scope_active_code` (`scope`, `active_menu_code`),
  UNIQUE KEY `uk_rbac_menu_id_scope` (`id`, `scope`),
  KEY `idx_rbac_menu_history_code` (`scope`, `menu_code`),
  KEY `idx_rbac_menu_parent_scope` (`parent_id`, `scope`),
  CONSTRAINT `fk_rbac_menu_parent_scope` FOREIGN KEY (`parent_id`, `scope`)
    REFERENCES `rbac_menu` (`id`, `scope`),
  CONSTRAINT `ck_rbac_menu_scope` CHECK (`scope` IN ('admin', 'business'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='统一RBAC-前端导航菜单表';

CREATE TABLE `rbac_subject_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主体角色关联主键',
  `subject_type` varchar(20) NOT NULL COMMENT '主体类型：admin-管理端账号，user-业务用户',
  `subject_id` bigint NOT NULL COMMENT '对应 admin_user 或 user 表中的稳定主键',
  `scope` varchar(20) NOT NULL COMMENT '主体及角色共同所属的权限域',
  `role_id` int NOT NULL COMMENT '统一角色主键',
  `update_time` datetime NOT NULL COMMENT '最后修改时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rbac_subject_role` (`subject_type`, `subject_id`, `role_id`),
  KEY `idx_rbac_subject_role_role_scope` (`role_id`, `scope`),
  CONSTRAINT `fk_rbac_subject_role_role_scope` FOREIGN KEY (`role_id`, `scope`)
    REFERENCES `rbac_role` (`id`, `scope`),
  CONSTRAINT `ck_rbac_subject_role_scope` CHECK (
    (`subject_type` = 'admin' AND `scope` = 'admin') OR
    (`subject_type` = 'user' AND `scope` = 'business')
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='统一RBAC-主体角色关联表';

CREATE TABLE `rbac_role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色权限关联主键',
  `scope` varchar(20) NOT NULL COMMENT '角色和权限共同所属的权限域',
  `role_id` int NOT NULL COMMENT '统一角色主键',
  `permission_id` int NOT NULL COMMENT '统一权限主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rbac_role_permission` (`role_id`, `permission_id`),
  KEY `idx_rbac_role_permission_role_scope` (`role_id`, `scope`),
  KEY `idx_rbac_role_permission_permission_scope` (`permission_id`, `scope`),
  CONSTRAINT `fk_rbac_role_permission_role_scope` FOREIGN KEY (`role_id`, `scope`)
    REFERENCES `rbac_role` (`id`, `scope`),
  CONSTRAINT `fk_rbac_role_permission_permission_scope` FOREIGN KEY (`permission_id`, `scope`)
    REFERENCES `rbac_permission` (`id`, `scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='统一RBAC-角色后端权限关联表';

CREATE TABLE `rbac_role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色菜单关联主键',
  `scope` varchar(20) NOT NULL COMMENT '角色和菜单共同所属的权限域',
  `role_id` int NOT NULL COMMENT '统一角色主键',
  `menu_id` int NOT NULL COMMENT '统一菜单主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rbac_role_menu` (`role_id`, `menu_id`),
  KEY `idx_rbac_role_menu_role_scope` (`role_id`, `scope`),
  KEY `idx_rbac_role_menu_menu_scope` (`menu_id`, `scope`),
  CONSTRAINT `fk_rbac_role_menu_role_scope` FOREIGN KEY (`role_id`, `scope`)
    REFERENCES `rbac_role` (`id`, `scope`),
  CONSTRAINT `fk_rbac_role_menu_menu_scope` FOREIGN KEY (`menu_id`, `scope`)
    REFERENCES `rbac_menu` (`id`, `scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='统一RBAC-角色前端菜单关联表';

CREATE TABLE `operate_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '业务端审计日志主键',
  `user_id` bigint DEFAULT NULL COMMENT '操作用户主键；匿名操作为空',
  `actor_type` varchar(30) DEFAULT NULL COMMENT '事件发生时的主体类型快照',
  `actor_account` varchar(191) DEFAULT NULL COMMENT '事件发生时的登录账号快照',
  `actor_name` varchar(100) DEFAULT NULL COMMENT '事件发生时的显示名称快照',
  `subject_account` varchar(191) DEFAULT NULL COMMENT '匿名请求尝试操作的账号',
  `subject_type` varchar(50) DEFAULT NULL COMMENT '被操作主体或资源类型',
  `subject_id` varchar(100) DEFAULT NULL COMMENT '被操作主体或资源稳定标识',
  `type` tinyint NOT NULL COMMENT '执行结果：1-成功，2-失败',
  `event_type` varchar(30) NOT NULL COMMENT '事件分类：AUTHENTICATION、OPERATION、SECURITY',
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
  KEY `idx_operate_log_actor_account` (`actor_account`),
  KEY `idx_operate_log_subject_account` (`subject_account`),
  KEY `idx_operate_log_subject` (`subject_type`, `subject_id`),
  KEY `idx_operate_log_type_time` (`type`, `create_time`),
  KEY `idx_operate_log_event_type_time` (`event_type`, `create_time`),
  KEY `idx_operate_log_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='业务端-操作审计日志表';

CREATE TABLE `operate_admin_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '管理端审计日志主键',
  `user_id` bigint DEFAULT NULL COMMENT '操作管理员主键；匿名操作为空',
  `actor_type` varchar(30) DEFAULT NULL COMMENT '事件发生时的主体类型快照',
  `actor_account` varchar(191) DEFAULT NULL COMMENT '事件发生时的登录账号快照',
  `actor_name` varchar(100) DEFAULT NULL COMMENT '事件发生时的显示名称快照',
  `subject_account` varchar(191) DEFAULT NULL COMMENT '匿名请求尝试操作的账号',
  `subject_type` varchar(50) DEFAULT NULL COMMENT '被操作主体或资源类型',
  `subject_id` varchar(100) DEFAULT NULL COMMENT '被操作主体或资源稳定标识',
  `type` tinyint NOT NULL COMMENT '执行结果：1-成功，2-失败',
  `event_type` varchar(30) NOT NULL COMMENT '事件分类：AUTHENTICATION、OPERATION、SECURITY',
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
  KEY `idx_operate_admin_log_actor_account` (`actor_account`),
  KEY `idx_operate_admin_log_subject_account` (`subject_account`),
  KEY `idx_operate_admin_log_subject` (`subject_type`, `subject_id`),
  KEY `idx_operate_admin_log_type_time` (`type`, `create_time`),
  KEY `idx_operate_admin_log_event_type_time` (`event_type`, `create_time`),
  KEY `idx_operate_admin_log_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='管理端-操作审计日志表';

INSERT INTO `rbac_role`
  (`scope`, `code`, `name`, `remark`, `built_in`, `super_role`, `is_delete`, `update_time`, `create_time`)
VALUES
  ('admin', 'admin:administrator', '管理端管理员', '承担日常用户和权限管理工作的后台角色', 0, 0, 0, NOW(), NOW()),
  ('admin', 'admin:super-admin', '超级管理员', '管理端最高权限角色，由代码授予权限通配符和全部管理菜单', 1, 1, 0, NOW(), NOW()),
  ('business', 'business:member', '普通业务用户', '注册或首次第三方登录时分配的默认业务角色', 0, 0, 0, NOW(), NOW()),
  ('business', 'business:operator', '业务操作员', '示例业务角色，可按具体项目继续拆分业务权限', 0, 0, 0, NOW(), NOW());

INSERT INTO `rbac_permission`
  (`scope`, `code`, `name`, `resource_type`, `description`, `status`, `is_delete`)
VALUES
  ('admin', 'admin:business-user:read', '查看业务用户', 'api', '允许管理端查询业务用户及其角色', 1, 0),
  ('admin', 'admin:business-user:role:write', '配置业务用户角色', 'api', '允许管理端替换业务用户的业务端角色', 1, 0),
  ('admin', 'admin:business-user:write', '管理业务用户', 'api', '允许启停和注销业务用户', 1, 0),
  ('admin', 'admin:admin-user:read', '查看管理员', 'api', '允许分页和查看管理员账号及角色', 1, 0),
  ('admin', 'admin:admin-user:write', '管理管理员', 'api', '允许新增、修改、启停、删除管理员及配置角色', 1, 0),
  ('admin', 'admin:rbac:read', '查看权限配置', 'api', '允许查询管理端和业务端的角色、权限及菜单', 1, 0),
  ('admin', 'admin:rbac:write', '修改权限配置', 'api', '允许修改角色的权限和菜单关联', 1, 0),
  ('admin', 'admin:audit-log:read', '查看管理端审计日志', 'api', '允许查询管理端操作审计日志', 1, 0);

INSERT INTO `rbac_menu`
  (`scope`, `parent_id`, `menu_code`, `menu_name`, `path`, `component`, `icon`, `sort`, `visible`, `status`, `is_delete`)
VALUES
  ('admin', NULL, 'admin-business-users', '业务用户管理', '/business/users', 'business/UserIndex', 'user', 10, 1, 1, 0),
  ('admin', NULL, 'admin-users', '管理员管理', '/system/admin-users', 'system/AdminUserIndex', 'admin', 15, 1, 1, 0),
  ('admin', NULL, 'admin-rbac', '权限管理', '/system/rbac', 'system/RbacIndex', 'lock', 20, 1, 1, 0),
  ('admin', NULL, 'admin-audit-log', '管理端审计日志', '/audit/log', 'audit/LogIndex', 'document', 100, 1, 1, 0);

INSERT INTO `rbac_role_permission` (`scope`, `role_id`, `permission_id`)
SELECT 'admin', r.id, p.id FROM `rbac_role` r JOIN `rbac_permission` p ON p.scope = 'admin'
WHERE r.scope = 'admin' AND r.code = 'admin:administrator';

INSERT INTO `rbac_role_menu` (`scope`, `role_id`, `menu_id`)
SELECT 'admin', r.id, m.id FROM `rbac_role` r JOIN `rbac_menu` m ON m.scope = 'admin'
WHERE r.scope = 'admin' AND r.code = 'admin:administrator';
