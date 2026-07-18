# 数据库与 Flyway

## 事实入口

先执行 `rg --files database-migrations`，再核对两个应用的 Flyway 配置、`db/README.md`、相关
Entity/Mapper 和架构测试。迁移目录中的文件、Git 历史和真实数据库状态共同决定当前版本，
不要从本文推断固定版本数量。

## 迁移所有权

- 共享迁移只放在 `database-migrations/src/main/resources/db/migration/shared`。
- admin 与 business 依赖同一迁移制品，并使用同一 Schema 和 `flyway_schema_history`。
- 两个部署连接同一目标 Schema 是运维前置条件，代码不会自动校验两个外部连接地址相同。
- 正常应用保持 `validate-on-migrate=true`，不启用 Flyway clean。

## 不可变历史

- 已进入基准或共享分支的迁移视为不可变，不修改内容、不重命名、不删除、不重新排序。
- 结构、约束或初始化数据变化先发现当前最大版本，再新增更高版本迁移。
- 只有尚未进入基准或共享分支，且未被共享或持久化环境执行的本地草稿，才能在用户明确授权下重写；可销毁的本地/Testcontainers 试跑不冻结草稿。
- `baseline-on-migrate` 不用于接管来源未知的非空库，`repair` 不用于掩盖未经评审的历史变更。
- `.cleanDisabled(false)` 只允许出现在隔离的测试容器中。

## 结构变更闭环

逐项覆盖：

1. Flyway SQL、初始化数据及中文表字段注释。
2. admin/business Entity、Mapper、XML 或注解 SQL。
3. Request/Response DTO、查询条件和对外兼容性。
4. 主键、唯一约束、普通索引、复合外键、CHECK 约束及删除策略。
5. 软删除后的唯一性和历史数据保留行为。
6. 每个字段的写入来源、读取方、维护责任和测试；不复制无来源的预留字段。
7. README、部署说明和数据库重建或升级要求。

建模使用稳定内部主键；账号、邮箱和手机号等可变标识不作为业务外键。RBAC 主体、角色、
权限和菜单通过 scope 复合约束隔离，不能只依赖 Java 校验。

## 完成门

涉及新迁移时，全部适用项必须有证据：

- 空库执行到最终结构，执行数和版本符合迁移目录现状。
- 从上一版本的真实结构和代表性数据升级成功，存量语义保持兼容。
- 再次 migrate 执行数为 0。
- 两个应用的 location、history table 和目标 Schema 契约一致；若承诺启动顺序无关，必须用同一数据库验证两个顺序。
- 关键唯一约束、复合外键、CHECK、软删唯一性和 bootstrap 在真实 MySQL 中生效。
- Testcontainers 报告由本次构建生成且没有跳过；Maven 退出码 0 不能单独证明完成。
