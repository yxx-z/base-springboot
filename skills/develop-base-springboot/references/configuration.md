# 配置、Profile 与启动方式

## 配置分层

- `application.yml`：应用名、端口、跨环境默认值和可覆盖的功能策略。
- `application-dev.yml`：本地开发差异。
- `application-prod.yml`：生产差异和必须注入的外部配置。
- `application-bootstrap.yml`：首个管理员初始化的最小启动模式。

把 `spring.application.name` 放在公共 application.yml，不在各 Profile 重复。

## 环境变量

- 生产数据库、Redis、RabbitMQ、SMTP、支付宝等凭据必须通过环境变量或密钥管理系统注入。
- 不新增明文生产凭据；发现仓库已有真实凭据时明确报告并建议轮换，不在输出中重复泄露。
- 使用 Spring Boot relaxed binding 的稳定键名，必要时在 YAML 中给出显式占位符和安全默认值。
- Duration 使用 `1s`、`5m`、`7d` 等明确单位。
- List 环境变量默认值需要确认 IDE YAML 解析和 Spring Binder 均可接受。

## Profile

- 默认开发 Profile 为 dev。
- 生产必须显式设置 `SPRING_PROFILES_ACTIVE=prod`。
- integration 由集成测试使用，不应连接开发共享基础设施。
- bootstrap 只用于空库初始化首个管理员，完成后主动关闭最小上下文。
- 不依赖“当前工作目录刚好是项目根目录”的隐式行为。

## Feature 开关

- Feature 开关控制 Bean、接口或行为是否启用。
- 关闭 Feature 不等于对应 Maven 依赖已经移除。
- 新 Feature 必须定义：默认值、关闭后的启动行为、接口行为、依赖 Bean 条件和测试覆盖。
- 测试需要功能开启时在测试属性中显式启用，不依赖应用默认值。

## bootstrap

- 使用独立 bootstrap Profile 和必要的 `BOOTSTRAP_*` 配置。
- 禁止在正常 dev/prod 启动中重复创建初始管理员。
- 初始化过程必须事务提交成功后再关闭上下文。
- 测试等待上下文关闭使用事件和线程同步，不使用 sleep 轮询。
- bootstrap 不需要 Redis、Web Server、常规 Session 和无关基础设施。

## 路径

- 相对日志路径相对于 JVM 当前工作目录，不等于仓库根目录。
- 生产日志、上传目录、临时目录等使用外部绝对路径。
- Classpath 资源使用 `classpath:`，不要拼接源码目录路径。

## RabbitMQ

- 使用 `common-mq` 的应用必须显式配置 `spring.rabbitmq.publisher-confirm-type=correlated` 和
  `spring.rabbitmq.publisher-returns=true`；缺失时可靠发布器应在启动阶段拒绝装配。
- 生产使用独立 vhost 和最小权限账号，禁止保留 guest 默认凭据。
- `framework.rabbitmq.publisher.confirm-timeout` 必须大于 0。
- 关闭公共发布器只移除 `RabbitMessagePublisher`，不等于移除 Spring AMQP 依赖或消费者能力。
