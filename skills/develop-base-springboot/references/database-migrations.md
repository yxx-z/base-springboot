# 数据库与 Flyway

## 当前基线

- 唯一共享迁移目录：`database-migrations/src/main/resources/db/migration/shared`。
- admin 与 business 使用同一 Schema、同一迁移制品和同一 `flyway_schema_history` 表。
- 当前项目处于初始基础框架阶段，最终基线为 `V1__init_shared_schema.sql`。
- 后续正式增量迁移从 V2 开始。

执行任务前用 `rg --files database-migrations` 确认现状，不假设版本数量永远不变。

## 历史迁移规则

- 未进入正式环境前，可以在明确要求下整理 V1，但必须说明已有开发数据库需要删除重建。
- 任何已进入共享、测试、预发布或生产环境的迁移都不可修改内容、重命名或重新排序。
- 正式环境变更只能新增更高版本迁移，不能通过 Flyway repair 掩盖未经评审的历史变更。
- 删除或合并迁移文件后必须 `clean` 构建，避免 `target/classes` 残留旧 SQL。

## 表结构修改清单

同步检查：

1. Flyway SQL 和初始化数据。
2. admin/business Entity、Mapper、XML/注解 SQL。
3. Request/Response DTO 和查询条件。
4. 唯一约束、普通索引、复合外键和 CHECK 约束。
5. 软删除后的唯一性行为。
6. 数据库字段注释和 Java 字段语义。
7. 真实 MySQL Testcontainers 测试。
8. README 中对迁移和兼容性的说明。

## 建模约定

- 使用稳定内部主键关联，不使用可变账号、邮箱或手机号作为业务外键。
- 软删除唯一键使用项目现有生成列方案，确保活跃记录唯一且历史记录可保留。
- RBAC 的主体、角色、权限必须通过 scope 复合约束隔离。
- 审计日志优先追加，不应提供普通业务删除语义。
- 避免模糊字段名；新字段明确状态、单位、主体和时间语义。
- 不保留没有写入来源、查询用途和近期路线图的预留字段。

## 审计表判断

区分三类空值：

- 匿名请求合理为空：actor ID、actor 账号和名称。
- 场景可选为空：subject、异常、请求参数、IP 归属地。
- 实际未接入：例如没有链路追踪组件时的 span ID。

发现大量空值时，先检查 `@AuditLog` 注解和事件转换，再决定补充采集还是删除字段，不根据单条记录直接删列。

## 验证

高风险迁移至少验证：

- 空库只执行预期迁移。
- 再次 migrate 执行数为 0。
- admin/business 任一应用先启动都得到相同最终 Schema。
- bootstrap 可在空库创建首个超级管理员。
- 关键唯一约束、复合外键和 CHECK 约束真实生效。
