# 数据库迁移说明

admin 是管理面，business 是业务面；两个应用必须连接同一个 MySQL Schema。数据库结构由
独立模块统一维护：

```text
database-migrations/src/main/resources/db/migration/shared
```

两个应用都依赖 `database-migrations`，并共同使用：

```text
flyway_schema_history
```

这样无论 admin 还是 business 先启动，都会获得管理员、业务用户、统一 RBAC 和审计日志
所需的完整表结构；并发启动时由 Flyway 数据库锁保证迁移串行执行。

当前迁移：

- `V1__init_shared_schema.sql`：一次性建立管理端账号、业务用户、软删唯一约束、统一 RBAC、安全不变量和两端审计表的最终初始结构。

迁移规范：

- 已经发布或提交到共享分支的迁移文件禁止修改。
- 后续变化必须新增更高版本，例如 `V2__add_order_schema.sql`。
- 表、字段必须提供中文 `COMMENT`。
- 必须明确主键、唯一约束、外键、`CHECK` 约束和必要索引。
- RBAC 关联必须保留 `scope` 复合外键，不能只依赖 Java 代码约定权限域。
- 必须同时验证空库完整迁移和从上一版本升级。
- 禁止在 admin、business 或领域模块中另建 Flyway 历史表。
- 禁止用可重复执行的全量建表脚本替代版本化迁移。

当前项目没有历史生产数据，首次使用应创建空数据库后直接启动任一应用，由 Flyway 自动
完成初始化。
