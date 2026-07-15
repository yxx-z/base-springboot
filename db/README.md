# 数据库迁移说明

数据库结构由各应用独立维护，禁止继续使用可重复执行的全量建表脚本。

- 用户端迁移：`business/src/main/resources/db/migration/business`
- 管理端迁移：`admin/src/main/resources/db/migration/admin`

两个应用分别使用 `flyway_schema_history_business` 和
`flyway_schema_history_admin`，因此既可以部署到独立数据库，也可以在过渡阶段共享同一数据库。

已经发布的迁移文件不得修改。后续结构变化必须新增更高版本的迁移，例如：

```text
V2__add_user_profile.sql
V3__add_admin_lock_status.sql
```

当前项目没有历史生产数据，首次使用应创建空数据库后直接启动应用，由 Flyway 自动完成初始化。
