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
│   ├── common-core       公共契约、模型、枚举、注解和跨应用复用能力
│   ├── common-security   统一认证主体、安全上下文、Sa-Token 适配和安全注解
│   └── common-framework  Web、安全、数据访问、缓存、日志和异步基础设施
├── business              用户端业务及支付宝等业务专属集成
├── admin                 管理端业务
└── db                    数据库基线脚本
```

模块依赖方向固定为：

```text
admin / business -> common-framework -> common-security -> common-core
```

约束原则：

- `common-core` 不允许依赖具体业务模块。
- `common-framework` 负责组装 Spring 基础设施，不承载用户、订单、支付等业务规则。
- 支付宝等业务专属 SDK 只能由 `business` 声明，不能进入管理端运行时依赖。
- 启动模块只声明自己实际需要的依赖，版本统一由根 POM 管理。

## 本地运行

1. 创建 MySQL 数据库并执行 `db/db.sql`。
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

## 基础设施约定

### 统一响应与异常

- Controller 使用 `@ResponseResult` 启用统一响应包装。
- 业务异常使用业务错误码，HTTP 状态码同时表达认证失败、权限不足、参数错误、冲突和服务端异常。
- 未知异常只向客户端返回通用消息，完整堆栈保留在服务端日志。

### 认证与密码

- 用户端和管理端使用独立 Sa-Token 登录体系。
- 权限查询必须根据 `loginType` 读取对应 Session。
- 注册、登录、修改密码和重置密码统一使用 BCrypt。
- `@AllowAnonymous` 可标注在 Controller 类或方法上，其他接口默认要求登录。
- 用户端采用“系统用户主体 + 多登录身份”模型，密码、支付宝等策略最终统一映射到内部用户 ID。
- 登录时生成角色和权限快照；角色或权限变更后必须注销受影响账号的全部会话。

### TraceId 与日志

- 每个 HTTP 请求都会生成或继承合法的 `Trace-Id`，并通过响应头返回。
- 请求结束后强制清理 ThreadLocal 和 MDC，防止容器线程复用导致串号。
- 请求参数进入日志前统一脱敏；密码、Token、授权头、密钥和签名不得明文记录。
- 访问日志不打印完整响应对象，避免敏感信息和大对象进入日志平台。

### 数据与缓存

- 分页大小上限为 200，MyBatis-Plus 同时在拦截器层执行最终限制。
- 禁止无条件全表更新和删除。
- 数据库脚本为账号、邮箱、角色编码和关联关系建立唯一约束及必要索引。
- Redis 连接由 Starter 统一管理，业务代码不得自行创建第二套 RedissonClient。
- Redis JSON 多态反序列化仅允许项目类型及必要 JDK 类型。

### 异步任务

- 禁止直接使用 `CompletableFuture` 公共线程池。
- 基础框架提供有界 `applicationTaskExecutor`，包含明确的容量、拒绝策略、优雅停机和 MDC 传播。

## 数据库升级提醒

`db/db.sql` 是新环境基线脚本。已有数据库升级前应先检查并清理重复账号、重复邮箱及重复角色关系，再通过正式迁移工具增加唯一约束和外键，不能直接在生产库重复执行基线脚本。
