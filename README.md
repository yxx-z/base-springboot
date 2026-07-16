# base-springboot

面向实际项目复用的 Spring Boot 3 多模块基础工程，统一提供认证鉴权、数据访问、缓存、异常协议、链路追踪、操作审计和工程质量约束。

## 环境要求

- JDK 17
- Maven 3.8.6 及以上
- MySQL 8
- Redis 6 及以上

构建阶段通过 Maven Enforcer 校验 Java、Maven 版本及依赖收敛情况，不符合要求时会直接终止构建。

## 模块边界

```text
base-springboot
├── common
│   ├── common-core       纯 Java 公共契约、错误码和基础工具
│   ├── common-cache      Redis、Redisson 和缓存序列化
│   ├── common-security   登录主体、Sa-Token、密码与会话安全
│   ├── common-web        Servlet、统一响应、JSON 和 Web 拦截器
│   ├── common-ip         可信代理客户端 IP 与归属地解析
│   ├── common-mail       邮件发送和邮件配置
│   ├── common-data       MyBatis-Plus 和审计字段填充
│   ├── common-audit      审计注解、事件和切面
│   └── common-framework  基础设施聚合入口、跨安全域协调器和异步执行器
├── business              用户端业务及支付宝 OAuth 登录
├── admin                 管理端业务
├── architecture-tests    跨应用共享库、bootstrap 与部署边界集成测试
└── db                    数据库迁移说明
```

模块依赖方向固定为：

```text
architecture-tests -> admin / business -> common-framework -> 各职责模块 -> common-core
```

约束原则：

- `common-core` 不允许依赖具体业务模块。
- `common-core` 不依赖 Spring Web、MyBatis、Redis、邮件和第三方平台 SDK。
- `common-framework` 只承载跨模块基础设施协调，不承载用户、订单、支付等领域规则。
- 支付宝等业务专属 SDK 只能由 `business` 声明，不能进入管理端运行时依赖。
- 基础框架只提供支付宝 OAuth 登录，不内置脱离订单领域的支付、退款或支付回调示例。
- 启动模块只声明自己实际需要的依赖，版本统一由根 POM 管理。
- `architecture-tests` 只参与测试，禁止被任何生产模块反向依赖。

## 本地运行

1. 创建空 MySQL 数据库。应用启动时由 Flyway 自动执行各自迁移。
2. 按实际环境维护 `application-dev.yml` 中的 MySQL、Redis、邮件配置。
3. 启动业务端：

   ```bash
   mvn -pl business -am spring-boot:run
   ```

4. 启动管理端：

   ```bash
   mvn -pl admin -am spring-boot:run
   ```

默认启用 `dev` 环境。生产部署必须显式设置：

```bash
export SPRING_PROFILES_ACTIVE=prod
export CORS_ALLOWED_ORIGIN_PATTERN=https://your-frontend.example.com
```

## 构建与测试

```bash
./mvnw clean verify
```

本地也可以使用已安装的 Maven 执行同样命令；团队和 CI 优先使用 `./mvnw`，确保 Maven 版本一致。

普通单元测试禁止连接开发机上的真实 MySQL、Redis 或邮件服务。需要完整基础设施的测试应放入独立集成测试阶段，并使用 Testcontainers 或 CI 服务容器提供依赖。

仓库包含 GitHub Actions 配置，每次推送和 Pull Request 都会在 JDK 17 下执行完整 `verify`。

打包后 business 和 admin 均生成普通 JAR 与 `-exec.jar`；普通 JAR 用于模块依赖和架构测试，
部署启动使用可执行的 `business-1.0.0-exec.jar`、`admin-1.0.0-exec.jar`。

## 基础设施约定

### 统一响应与异常

- Controller 使用 `@ResponseResult` 启用统一响应包装。
- 业务异常使用业务错误码，HTTP 状态码同时表达认证失败、权限不足、参数错误、冲突和服务端异常。
- 未知异常只向客户端返回通用消息，完整堆栈保留在服务端日志。

### 认证与密码

- 用户端和管理端使用独立 Sa-Token 登录体系。
- Token 使用 Redis 有状态 Session；用户端最长 7 天、空闲 2 小时，管理端最长 8 小时、空闲 30 分钟。
- 权限查询必须根据 `loginType` 读取对应 Session。
- 注册、登录、修改密码和重置密码统一使用 BCrypt。
- 登录账号和邮箱在查询、缓存 Key 生成和持久化前统一规范化；第三方身份保持平台原始标识。
- 新密码由 `security.password-policy` 配置长度、BCrypt 字节上限和复杂度，默认最少 12 位且最多 72 个 UTF-8 字节。
- `@AllowAnonymous` 可标注在 Controller 类或方法上，其他接口默认要求登录。
- 用户端采用“系统用户主体 + 多登录身份”模型，密码、支付宝等策略最终统一映射到内部用户 ID。
- 登录身份必须同时满足 `status=true` 和 `verified=true`。
- 登录时生成角色和权限快照；角色或权限变更后必须注销受影响账号的全部会话。
- 超级管理员角色属于内置安全角色，拥有权限通配符和全部有效菜单，禁止修改其权限或菜单关联。
- 系统始终至少保留一个启用的超级管理员，停用、删除或移除最后一个超级管理员角色会被拒绝。

### TraceId 与日志

- 每个 HTTP 请求都会生成或继承合法的 `Trace-Id`，并通过响应头返回。
- 请求结束后强制清理 ThreadLocal 和 MDC，防止容器线程复用导致串号。
- 请求参数进入日志前统一脱敏；密码、Token、授权头、密钥和签名不得明文记录。
- Controller 访问日志默认关闭，可通过 `WEB_ACCESS_LOG_ENABLED=true` 开启；业务审计由显式 `@AuditLog` 控制。
- 审计日志保存事件发生时的主体类型、账号和名称快照，历史查询不依赖当前用户表。
- 失败登录只记录尝试登录的账号，不记录密码、验证码或 Token。

### 数据与缓存

- 分页大小上限为 200，MyBatis-Plus 同时在拦截器层执行最终限制。
- Controller 统一返回框架自己的 `PageResponse<T>`，不暴露 MyBatis-Plus 分页类型。
- 禁止无条件全表更新和删除。
- business 和 admin 分别维护 Flyway 迁移与历史表，可以使用独立数据库，也可在过渡期共享数据库。
- Redis 连接由 Starter 统一管理，业务代码不得自行创建第二套 RedissonClient。
- Redis JSON 多态反序列化仅允许项目类型及必要 JDK 类型。
- 登录失败次数通过 Redis Lua 原子预占，账号和 IP 两个维度不会在并发下越过限制。
- 一次性密码重置 Token 使用摘要作为 Redis Key，事务提交后保留已消费标记至原 Token 过期。

### 导航菜单

- 用户端和管理端分别维护独立菜单和角色菜单表。
- `status=false` 表示菜单不可用；`visible=false` 只表示不在导航中展示。
- 可见子菜单位于隐藏父菜单下时，会提升到最近的可见祖先；不存在可见祖先时提升为根节点。
- 菜单树使用公共纯 Java 构建器生成，不修改 MyBatis 持久化实体。

### 接口路径

- 接口路径统一使用 kebab-case，例如 `/send-captcha`、`/reset-password` 和 `/change-password`。
- 当前工程处于架构阶段，不保留旧 camelCase 路径兼容入口。

### 异步任务

- 禁止直接使用 `CompletableFuture` 公共线程池。
- 基础框架提供有界 `applicationTaskExecutor` 和独立的 `auditTaskExecutor`，包含明确的容量、拒绝策略、优雅停机和 MDC 传播。

## 数据库升级提醒

已经发布的 Flyway 迁移文件禁止修改。所有表、字段、索引和初始化数据变化都必须新增更高版本迁移，详细约定见 `db/README.md`。
