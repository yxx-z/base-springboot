-- 用户、管理员和 RBAC 配置均采用逻辑删除。唯一约束只限制“当前有效记录”，历史软删记录
-- 继续保留原始账号、手机号及第三方身份标识，既允许重新注册，也为反复注销风控保留依据。

ALTER TABLE `user`
    DROP INDEX `uk_user_email`,
    DROP INDEX `idx_user_phone`,
    ADD COLUMN `active_email` varchar(100)
        GENERATED ALWAYS AS (CASE WHEN `is_delete` = 0 THEN `email` ELSE NULL END) STORED
        COMMENT '仅未删除用户参与唯一约束的邮箱；软删记录为NULL',
    ADD COLUMN `active_phone` varchar(20)
        GENERATED ALWAYS AS (CASE WHEN `is_delete` = 0 THEN `phone` ELSE NULL END) STORED
        COMMENT '仅未删除用户参与唯一约束的手机号；软删记录为NULL',
    ADD UNIQUE KEY `uk_user_active_email` (`active_email`),
    ADD UNIQUE KEY `uk_user_active_phone` (`active_phone`),
    ADD KEY `idx_user_history_email` (`email`),
    ADD KEY `idx_user_history_phone` (`phone`);

ALTER TABLE `user_identity`
    DROP INDEX `uk_user_identity_type_identifier`,
    ADD COLUMN `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    ADD COLUMN `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0
        COMMENT '逻辑删除标记：0-未删除，1-已删除',
    ADD COLUMN `active_identifier` varchar(191)
        GENERATED ALWAYS AS (CASE WHEN `is_delete` = 0 THEN `identifier` ELSE NULL END) STORED
        COMMENT '仅未删除身份参与唯一约束的身份标识；软删记录为NULL',
    ADD UNIQUE KEY `uk_user_identity_active_identifier`
        (`identity_type`, `active_identifier`),
    ADD KEY `idx_user_identity_history_identifier` (`identity_type`, `identifier`),
    ADD KEY `idx_user_identity_user_delete` (`user_id`, `is_delete`);

ALTER TABLE `admin_user`
    DROP INDEX `uk_admin_user_login_code`,
    DROP INDEX `uk_admin_user_email`,
    ADD COLUMN `active_login_code` varchar(50)
        GENERATED ALWAYS AS (CASE WHEN `is_delete` = 0 THEN `login_code` ELSE NULL END) STORED
        COMMENT '仅未删除管理员参与唯一约束的登录账号；软删记录为NULL',
    ADD COLUMN `active_email` varchar(100)
        GENERATED ALWAYS AS (CASE WHEN `is_delete` = 0 THEN `email` ELSE NULL END) STORED
        COMMENT '仅未删除管理员参与唯一约束的邮箱；软删记录为NULL',
    ADD COLUMN `active_link_phone` varchar(20)
        GENERATED ALWAYS AS (CASE WHEN `is_delete` = 0 THEN `link_phone` ELSE NULL END) STORED
        COMMENT '仅未删除管理员参与唯一约束的手机号；软删记录为NULL',
    ADD UNIQUE KEY `uk_admin_user_active_login_code` (`active_login_code`),
    ADD UNIQUE KEY `uk_admin_user_active_email` (`active_email`),
    ADD UNIQUE KEY `uk_admin_user_active_link_phone` (`active_link_phone`),
    ADD KEY `idx_admin_user_history_login_code` (`login_code`),
    ADD KEY `idx_admin_user_history_email` (`email`),
    ADD KEY `idx_admin_user_history_link_phone` (`link_phone`);

-- RBAC 配置软删后允许重新使用相同编码。生成列为 NULL 的历史记录不会互相产生唯一冲突。
ALTER TABLE `rbac_role`
    DROP INDEX `uk_rbac_role_scope_code`,
    DROP CHECK `ck_rbac_role_super_scope`,
    ADD COLUMN `active_code` varchar(100)
        GENERATED ALWAYS AS (CASE WHEN `is_delete` = 0 THEN `code` ELSE NULL END) STORED
        COMMENT '仅未删除角色参与唯一约束的角色编码；软删记录为NULL',
    ADD UNIQUE KEY `uk_rbac_role_scope_active_code` (`scope`, `active_code`),
    ADD KEY `idx_rbac_role_history_code` (`scope`, `code`),
    ADD CONSTRAINT `ck_rbac_role_super_invariant` CHECK (
        (`super_role` = 0 AND `code` <> 'admin:super-admin') OR
        (`super_role` = 1 AND `built_in` = 1 AND `scope` = 'admin'
            AND `code` = 'admin:super-admin')
    );

ALTER TABLE `rbac_permission`
    DROP INDEX `uk_rbac_permission_scope_code`,
    ADD COLUMN `active_code` varchar(150)
        GENERATED ALWAYS AS (CASE WHEN `is_delete` = 0 THEN `code` ELSE NULL END) STORED
        COMMENT '仅未删除权限参与唯一约束的权限编码；软删记录为NULL',
    ADD UNIQUE KEY `uk_rbac_permission_scope_active_code` (`scope`, `active_code`),
    ADD KEY `idx_rbac_permission_history_code` (`scope`, `code`);

ALTER TABLE `rbac_menu`
    DROP INDEX `uk_rbac_menu_scope_code`,
    ADD COLUMN `active_menu_code` varchar(100)
        GENERATED ALWAYS AS (CASE WHEN `is_delete` = 0 THEN `menu_code` ELSE NULL END) STORED
        COMMENT '仅未删除菜单参与唯一约束的菜单编码；软删记录为NULL',
    ADD UNIQUE KEY `uk_rbac_menu_scope_active_code` (`scope`, `active_menu_code`),
    ADD KEY `idx_rbac_menu_history_code` (`scope`, `menu_code`);

-- 审计目标与操作人是两个概念。主体类型和主体主键独立保存，不能强行塞入账号字段。
ALTER TABLE `operate_log`
    ADD COLUMN `subject_type` varchar(50) DEFAULT NULL COMMENT '被操作主体或资源类型' AFTER `subject_account`,
    ADD COLUMN `subject_id` varchar(100) DEFAULT NULL COMMENT '被操作主体或资源稳定标识' AFTER `subject_type`,
    ADD KEY `idx_operate_log_subject` (`subject_type`, `subject_id`);

ALTER TABLE `operate_admin_log`
    ADD COLUMN `subject_type` varchar(50) DEFAULT NULL COMMENT '被操作主体或资源类型' AFTER `subject_account`,
    ADD COLUMN `subject_id` varchar(100) DEFAULT NULL COMMENT '被操作主体或资源稳定标识' AFTER `subject_type`,
    ADD KEY `idx_operate_admin_log_subject` (`subject_type`, `subject_id`);

-- 业务端普通角色不得查看全局审计日志。日志仍正常落库，统一查询入口只保留在管理端；
-- 将来若提供“我的操作记录”，必须由服务端按当前登录 userId 强制过滤。
DELETE rpm
FROM `rbac_role_permission` rpm
JOIN `rbac_permission` permission ON permission.id = rpm.permission_id
WHERE permission.scope = 'business' AND permission.code = 'business:audit-log:read';

DELETE rmm
FROM `rbac_role_menu` rmm
JOIN `rbac_menu` menu ON menu.id = rmm.menu_id
WHERE menu.scope = 'business' AND menu.menu_code = 'business-audit-log';

UPDATE `rbac_permission`
SET `status` = 0, `is_delete` = 1
WHERE `scope` = 'business' AND `code` = 'business:audit-log:read';

UPDATE `rbac_menu`
SET `status` = 0, `is_delete` = 1
WHERE `scope` = 'business' AND `menu_code` = 'business-audit-log';

-- 补齐管理端完整账号管理所需权限和导航入口。
INSERT INTO `rbac_permission`
    (`scope`, `code`, `name`, `resource_type`, `description`, `status`, `is_delete`)
VALUES
    ('admin', 'admin:admin-user:read', '查看管理员', 'api', '允许分页和查看管理员账号及角色', 1, 0),
    ('admin', 'admin:admin-user:write', '管理管理员', 'api', '允许新增、修改、启停、删除管理员及配置角色', 1, 0),
    ('admin', 'admin:business-user:write', '管理业务用户', 'api', '允许启停和注销业务用户', 1, 0);

INSERT INTO `rbac_menu`
    (`scope`, `parent_id`, `menu_code`, `menu_name`, `path`, `component`, `icon`,
     `sort`, `visible`, `status`, `is_delete`)
VALUES
    ('admin', NULL, 'admin-users', '管理员管理', '/system/admin-users',
     'system/AdminUserIndex', 'admin', 15, 1, 1, 0);

INSERT INTO `rbac_role_permission` (`scope`, `role_id`, `permission_id`)
SELECT 'admin', role.id, permission.id
FROM `rbac_role` role
JOIN `rbac_permission` permission ON permission.scope = 'admin'
WHERE role.scope = 'admin' AND role.code = 'admin:administrator'
  AND permission.code IN (
      'admin:admin-user:read', 'admin:admin-user:write', 'admin:business-user:write'
  );

INSERT INTO `rbac_role_menu` (`scope`, `role_id`, `menu_id`)
SELECT 'admin', role.id, menu.id
FROM `rbac_role` role
JOIN `rbac_menu` menu ON menu.scope = 'admin'
WHERE role.scope = 'admin' AND role.code = 'admin:administrator'
  AND menu.menu_code = 'admin-users';
