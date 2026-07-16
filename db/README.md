# 数据库迁移说明

数据库结构由各应用独立维护，禁止继续使用可重复执行的全量建表脚本。

- 用户端迁移：`business/src/main/resources/db/migration/business`
- 管理端迁移：`admin/src/main/resources/db/migration/admin`

两个应用分别使用 `flyway_schema_history_business` 和
`flyway_schema_history_admin`，因此既可以部署到独立数据库，也可以在过渡阶段共享同一数据库。
共享同一 Schema 时，后启动的应用会以版本 `0` 创建自己的基线记录，然后继续完整执行
本应用的 V1 及后续迁移；版本 `0` 不是跳过 V1 的生产基线。

已经发布的迁移文件不得修改。后续结构变化必须新增更高版本的迁移，例如：

```text
V2__add_user_profile.sql
V3__add_admin_lock_status.sql
```

当前项目没有历史生产数据，首次使用应创建空数据库后直接启动应用，由 Flyway 自动完成初始化。

当前增量迁移：

- `business/V2__add_audit_actor_snapshot.sql`：增加用户端审计主体快照字段和索引。
- `admin/V2__add_audit_actor_snapshot.sql`：增加管理端审计主体快照字段和索引。
