# 测试与交付验证

## 环境与命令入口

- 根 `pom.xml`、Maven Enforcer 和 `.mvn/wrapper` 是 JDK、Maven 与依赖版本的事实来源。
- 所有 Maven 操作使用 `./mvnw`；稳定验证入口为 `skills/develop-base-springboot/scripts/verify-project.sh`。
- MySQL/Redis 集成测试使用 Testcontainers。严格模式需要可用容器运行时和本次构建生成的测试报告。

验证脚本模式：

- `static`：补丁格式、迁移不可变性、弃用导入和工作区静态检查。
- `compile`：全模块编译，不执行测试。
- `module LIST`：测试以逗号分隔的规范模块路径及其上游依赖，并要求每个显式模块本次实际执行测试。
- `architecture`：严格执行共享 Schema 和 bootstrap 测试。
- `full`：严格执行全量 `clean verify`，要求每个符合 Surefire 命名规则的测试源码生成报告，并确认容器测试清单完整执行。

`LIST` 只接受仓库内包含 `pom.xml` 的相对模块路径，例如 `common/common-security,admin`；不接受 Maven 的
`:artifactId` 等别名。`-am` 只加入上游依赖，不会自动运行下游消费者。修改 common 模块时，先从 POM
建立消费者清单，再显式选择有测试的受影响消费者或应用。没有测试的变更模块不作为 `LIST` 显式项，
由这些下游模块通过 `-am` 纳入编译；若没有可覆盖它的有测试消费者，使用 `compile` 并在交付前执行 `full`。

`static` 默认以 `origin/HEAD` 作为已提交迁移的比较基准；CI 或其他分支布局通过 `VERIFY_BASE_REF`
显式传入目标基准引用。CI 缺少两者时直接失败；本地没有可用基准时仍检查索引和工作区，并明确提示
尚未覆盖分支提交历史。

## 风险矩阵

| 修改类型 | 最低验证 |
|---|---|
| Skill、文档、纯静态规则 | Skill 格式校验、脚本语法检查、`static` |
| 纯工具、DTO、单个 Service | 对应 `module` 单元测试 |
| common 模块 | `module` 选择该模块及有测试的受影响消费者/应用；无测试中间模块由 `-am` 编译，最终执行 `full` |
| Controller、统一响应 | 对应应用集成测试 |
| 登录、密码、Token、Session、RBAC | admin、business 安全集成测试及相关单元测试 |
| Redis、并发消费、频控 | 真实 Redis Testcontainers 测试 |
| Forest Client、超时、Header、序列化 | common-http-client MockWebServer 测试及调用方测试 |
| 表结构、Flyway、约束、bootstrap | `architecture`，新增版本同时验证上一版本升级 |
| POM、模块边界、共享基础设施 | `full`，并执行对应边界检查 |
| Logback、Profile、生产路径 | 打包、非生产启动和 prod 等价日志冒烟 |

## 严格完成语义

- Maven 退出码 0 只表示 Maven 未报告失败，不证明目标测试存在或 Testcontainers 已执行。
- 测试模式运行前清除已有 Surefire XML（包括同名符号链接）；后续只接受本次重新生成的普通文件报告。
- 严格模式要求预期 Surefire XML 存在、由本次构建生成，并满足 `tests > 0`、`skipped=0`、`failures=0`、`errors=0`。
- `module` 通过清理已知容器测试类识别 `-am` 实际编译的上游容器套件；`full` 逐一核对默认 Surefire 命名测试源，不能由其他模块的报告托底。
- Docker 不可用、目标测试改名或零匹配、测试被禁用都属于验证未完成，不能报告成功。
- 删除或合并资源后使用 clean，避免 `target/classes` 残留旧 SQL、XML 或配置。
- `compile` 和 `module` 是增量入口；涉及删除、重命名、迁移或资源替换时，最终验证必须使用相应 clean 模式。
- 数据库约束、Redis 原子操作、Session 持久化和 HTTP 序列化使用真实基础设施或本地协议服务器验证，不能只证明 Mock 调用。
- 异步测试使用事件、Latch、Future、调度器或阻塞原语，不用固定 sleep。

## 收尾与交付

执行 `static` 后再检查 `git status --short`，确认旧引用、弃用 API、意外日志、敏感配置和无关文件均已处理。
Skill 变更同时使用当前可用 `skill-creator` 的格式校验；实质性 Router、行为规范或验证脚本变更还要对代表性任务做新鲜线程前向测试，纯措辞修正可说明理由后跳过。

交付报告列出实际命令、通过项、跳过项、环境阻塞、兼容性影响和真实风险，不写死历史测试总数。
