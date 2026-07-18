# 配置、Profile 与启动方式

## 事实入口

先定位两个应用的 `application*.yml`、`logback-spring.xml`、`@ConfigurationProperties`、
`@ConditionalOnProperty` 和对应测试。YAML、条件注解和测试共同决定实际默认行为。

## 配置分层

- `application.yml`：应用名、端口、跨环境默认值和可覆盖策略。
- `application-dev.yml`：本地开发差异，不承载共享环境真实凭据。
- `application-prod.yml`：生产差异；秘密和环境相关端点只引用外部注入值，稳定且非敏感的差异可以保留字面量。
- `application-bootstrap.yml`：首个管理员初始化所需的最小配置。

`spring.application.name` 放在公共配置中。默认开发 Profile 为 dev；生产显式设置
`SPRING_PROFILES_ACTIVE=prod`；integration 只连接测试隔离资源。

## 凭据与环境变量

- 所有 Git 跟踪的 Profile 只保存非敏感默认值、占位符和公开配置，不保存真实数据库、Redis、SMTP、第三方平台或密钥系统凭据。
- dev/local 的真实连接信息也通过环境变量、密钥工具或明确忽略的本地文件提供。
- 发现已提交凭据时按泄露处理：报告位置、建议轮换并外部化，输出中不复述具体值。
- 环境变量使用稳定的 relaxed binding 名称；Duration 始终带单位，List 默认值同时验证 YAML 解析和 Spring Binder。

## Feature 三态契约

每个 Feature 都定义一个权威默认值，并保持以下位置一致：

- YAML 占位符默认值。
- `@ConditionalOnProperty` 的 `havingValue` 和 `matchIfMissing`。
- Bean、Controller、接口错误及启动行为。

测试覆盖属性缺失、显式 `false`、显式 `true` 三种状态。需要开启能力的测试显式设置属性，
不借用应用默认值。关闭 Feature 只改变运行行为，不代表 Maven 依赖已经移除。

## bootstrap

- bootstrap 只在管理员表为空时创建首个管理员，不在 dev/prod 重复执行。
- 成功日志及依赖提交结果的副作用放在 `afterCommit`；上下文在 `afterCompletion` 关闭，失败时也释放资源。
- 最小上下文不装配 Web Server、Redis、常规 Session、邮件和无关基础设施。
- 测试通过事件、Latch、Future 或线程终止信号等待关闭，不轮询休眠。

## 路径与生产约束

- Classpath 资源使用 `classpath:`，文件系统相对路径始终按 JVM 当前工作目录解释。
- 本地可保留安全的相对路径默认值；生产日志、上传和临时目录必须注入外部绝对路径并做启动或部署校验。
- 修改 Profile、Logback 或路径后，验证至少一个非生产 Profile 和 prod 等价配置，确认没有意外文件、错误目录或敏感信息。

## 完成门

配置任务完成前，对本次触及的配置逐项证明默认值、三态行为、外部化凭据、Profile 隔离、关闭行为和对应测试一致。
