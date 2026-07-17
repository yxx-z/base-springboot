# 测试与交付验证

## 基本环境

- 使用 JDK 17。
- 使用 `./mvnw`，不要依赖机器全局 Maven 版本。
- 需要 MySQL/Redis 的测试使用 Testcontainers 和本机 Docker。
- Testcontainers 2.x 的 MySQL 类为 `org.testcontainers.mysql.MySQLContainer`，不使用已弃用旧包。
- RabbitMQ 集成测试使用 `org.testcontainers.rabbitmq.RabbitMQContainer`。

## 风险矩阵

| 修改类型 | 最低验证 |
|---|---|
| 纯工具、DTO、单个 Service | 对应模块单元测试 |
| common 模块 | 该模块及直接使用它的应用测试 |
| Controller、统一响应 | 对应应用集成测试 |
| 登录、密码、Token、Session、RBAC | admin/business 安全集成测试和相关单元测试 |
| Redis、并发消费、频控 | 真实 Redis Testcontainers 测试 |
| Forest Client、超时、Header 和序列化 | 本地 MockWebServer 集成测试 |
| RabbitMQ 发布、路由和 JSON 消息 | 真实 RabbitMQ Testcontainers 测试 |
| 表结构、Flyway、约束 | 真实 MySQL 架构测试 |
| bootstrap | AdminBootstrapIntegrationTest |
| POM、模块边界、共享基础设施 | 全量 `clean verify` |
| Logback、Profile | 打包、非生产启动和 prod 日志冒烟 |

## 推荐命令

编译：

```bash
./mvnw -q -DskipTests compile
```

模块测试：

```bash
./mvnw -q -pl <module-list> -am test
```

架构级数据库与 bootstrap 测试：

```bash
./mvnw -q clean -pl architecture-tests -am \
  -Dtest=SharedDatabaseFlywayIntegrationTest,AdminBootstrapIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

全量验证：

```bash
./mvnw -q clean verify
```

## 常见陷阱

- 删除或合并资源后未 clean，导致 target/classes 仍包含旧 SQL、XML 或配置。
- `@Testcontainers(disabledWithoutDocker = true)` 因 Docker 不可用跳过测试，但 Maven 仍返回 0。
- 定向 `-Dtest` 在多模块 reactor 中让无匹配测试模块失败；按需使用 `surefire.failIfNoSpecifiedTests=false`。
- 只验证 Mock 行为，没有验证 MySQL 约束、Redis Lua 或 Session 持久化。
- Forest 只验证接口代理创建，没有通过本地 HTTP Server 检查真实 URL、Header、请求体和响应反序列化。
- 用固定 sleep 等待异步结果，产生竞态和慢测试。
- 只编译当前模块，没有验证公共模块的消费者。

## 收尾检查

至少执行：

```bash
git diff --check
git status --short
```

并按修改内容检查：

- 删除或重命名后的旧引用。
- 弃用 API。
- 旧迁移名称和 target 残留。
- 测试是否真实执行而非跳过。
- 是否生成新的项目目录日志或敏感配置。
- 变更范围是否包含无关用户文件。

交付报告不写死历史测试数量；报告本次实际命令、通过情况、跳过情况和无法执行的真实原因。
