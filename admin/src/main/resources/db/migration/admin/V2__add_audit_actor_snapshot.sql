-- 审计日志保存事件发生时的主体快照，历史查询不再依赖当前管理员表。
ALTER TABLE `operate_admin_log`
    ADD COLUMN `actor_type` varchar(30) DEFAULT NULL COMMENT '事件发生时的主体类型快照' AFTER `user_id`,
    ADD COLUMN `actor_account` varchar(191) DEFAULT NULL COMMENT '事件发生时的登录账号快照' AFTER `actor_type`,
    ADD COLUMN `actor_name` varchar(100) DEFAULT NULL COMMENT '事件发生时的显示名称快照' AFTER `actor_account`,
    ADD COLUMN `subject_account` varchar(191) DEFAULT NULL COMMENT '匿名请求尝试操作的账号' AFTER `actor_name`,
    ADD KEY `idx_operate_admin_log_actor_account` (`actor_account`),
    ADD KEY `idx_operate_admin_log_subject_account` (`subject_account`);
