# base-springboot

面向实际项目复用的 Spring Boot 3 多模块基础工程，统一提供认证鉴权、数据访问、缓存、异常协议、链路追踪、操作审计、邮件通知和工程质量约束。

本项目不是一个包含演示业务的脚手架，而是一套可以继续扩展领域模块的后端基础架构。使用时应保留模块边界、安全不变量和数据库迁移规范，不建议把业务代码继续堆放到 `common` 模块。

## 技术栈与环境要求

- JDK 17
- Spring Boot 3.5.3
- Maven 3.8.6 及以上，仓库已提供 Maven Wrapper
- MySQL 8
- Redis 6 及以上
- MyBatis-Plus 3.5.12
- Sa-Token
- Flyway
- Redisson
- Testcontainers

构建阶段通过 Maven Enforcer 校验 Java、Maven 版本及依赖收敛情况，不符合要求时会直接终止构建。

## 模块结构

```text
base-springboot
├── common
│   ├── common-core       纯 Java 公共契约、错误码和基础工具
│   ├── common-cache      Redis、Redisson 和缓存原子操作
│   ├── common-security   登录主体、Sa-Token、密码与会话安全
│   ├── common-web        Servlet、统一响应、JSON 和 Web 拦截器
│   ├── common-ip         可信代理客户端 IP 与归属地解析
│   ├── common-mail       邮件发送和邮件配置
│   ├── common-data       MyBatis-Plus 和审计字段填充
│   ├── common-audit      审计注解、事件和切面
│   ├── common-rbac       统一 RBAC 数据模型、权限域规则和授权实现
│   └── common-framework  基础设施聚合入口、跨安全域协调器和异步执行器
├── database-migrations   admin/business 共库使用的统一 Flyway 迁移制品
├── business              业务端应用、用户认证、业务授权消费和支付宝 OAuth 登录
├── admin                 管理端应用、业务用户管理和统一权限配置入口
├── architecture-tests    共享 Schema、bootstrap 与部署边界集成测试
└── db                    数据库迁移规范说明
```

模块依赖方向固定为：

```text
architecture-tests -> admin / business -> common-rbac / common-framework -> 各职责模块 -> common-core
```

必须遵守以下边界：

- `common-core` 不依赖 Spring Web、MyBatis、Redis、邮件和第三方平台 SDK。
- `common-framework` 只承载跨模块基础设施协调，不承载用户、订单、支付等领域规则。
- `common-security` 定义认证、会话和授权提供器抽象，不依赖具体 RBAC 表。
- `common-rbac` 实现授权提供器并维护统一角色、权限和菜单规则，不承载管理端接口。
- 支付宝等业务专属 SDK 只能由使用它的业务模块声明。
- `business` 只消费 business 权限域；角色、权限和菜单管理入口统一放在 `admin`。
- `admin` 与 `business` 可以独立运行和发布，但必须连接同一个 MySQL Schema。
- 数据库迁移只能放入 `database-migrations`，保证任一应用先启动都能得到完整表结构。
- `architecture-tests` 只参与测试，禁止被任何生产模块反向依赖。
- 新业务优先建立独立模块；只有多个应用都需要且与领域无关的能力才允许下沉到 `common`。

## 快速开始

### 1. 准备基础设施

创建一个由管理端和业务端共用的空数据库，例如：

```sql
CREATE DATABASE base_app CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

`business` 和 `admin` 的数据源 URL 必须指向这个相同 Schema。两个应用依赖同一个
`database-migrations` 制品，并共用 `flyway_schema_history`；无论哪个应用先启动，Flyway
都会创建业务用户、管理员、统一 RBAC 和审计日志所需的完整结构。两个应用并发启动时由
Flyway 数据库锁保证迁移只执行一次。

Redis 默认同时承担业务缓存和 Sa-Token Session 存储。两类数据应使用不同 Redis database 或独立实例，具体连接信息在各应用的 `application-dev.yml` 中配置。

### 2. 配置开发环境

维护以下文件中的本地配置：

- `business/src/main/resources/application-dev.yml`
- `admin/src/main/resources/application-dev.yml`

至少需要确认：

- MySQL URL、用户名和密码
- Redis 地址、端口、database 和密码
- Sa-Token 独立 Redis 配置
- 邮件服务器、发件人和模板
- 支付宝应用配置，仅 `business` 需要
- IP 可信代理列表及是否启用归属地检查

仓库默认启用 `dev` Profile。生产环境必须显式设置：

```bash
export SPRING_PROFILES_ACTIVE=prod
export CORS_ALLOWED_ORIGIN_PATTERN=https://your-frontend.example.com
```

生产配置不得直接沿用开发环境的数据库、Redis、邮件密钥和第三方平台私钥。

### 3. 启动应用

启动用户端，默认端口 `6059`：

```bash
./mvnw -pl business -am spring-boot:run
```

启动管理端，默认端口 `6060`：

```bash
./mvnw -pl admin -am spring-boot:run
```

首次启动时 Flyway 会自动创建表、索引、基础角色、权限和菜单数据，不需要手工执行全量 SQL。

### 4. 初始化首个管理员

管理端提供一次性 `bootstrap` Profile。它只允许在管理员表为空时执行，并会将首个管理员绑定到内置超级管理员角色。

```bash
export SPRING_PROFILES_ACTIVE=bootstrap
export BOOTSTRAP_DB_URL='jdbc:mysql://127.0.0.1:3306/base_app?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai'
export BOOTSTRAP_DB_USERNAME='root'
export BOOTSTRAP_DB_PASSWORD='your-password'
export BOOTSTRAP_ADMIN_LOGIN_CODE='root-admin'
export BOOTSTRAP_ADMIN_LOGIN_NAME='系统管理员'
export BOOTSTRAP_ADMIN_EMAIL='admin@example.com'
export BOOTSTRAP_ADMIN_PASSWORD='Framework2026'

./mvnw -pl admin -am spring-boot:run
```

初始化完成后进程会自动退出。临时密码必须符合当前 `security.password-policy`，并应在首次登录后立即修改。

bootstrap 具有以下安全限制：

- 管理员表已有任何数据时拒绝运行。
- 超级管理员角色必须已经由 Flyway 初始化。
- 账号、角色关联在同一事务中创建。
- 不启动 Web、Redis、Sa-Token 等常规运行时基础设施。

### 5. 调用接口

用户端登录示例：

```bash
curl -X POST 'http://localhost:6059/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"loginCode":"your-account","password":"your-password"}'
```

统一响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "..."
  },
  "traceId": "..."
}
```

携带 Token 访问受保护接口：

```bash
curl 'http://localhost:6059/user/info' \
  -H 'Authorization: Bearer your-token'
```

用户端和管理端属于不同 Sa-Token 安全域，用户 Token 不能访问管理端接口，管理端 Token 也不能作为用户 Token 使用。

## 注册、登录与找回密码调用链

以下流程以用户端 `business` 为例，默认地址为 `http://localhost:6059`。管理端没有开放注册入口，管理员登录和找回密码分别使用管理端同名接口，并运行在独立的 `admin` 安全域。

### 前后端统一交互约定

前端对接前应先统一以下规则：

- 所有请求和响应使用 UTF-8 JSON。
- 不能只根据响应体中的 `code` 判断网络层状态，必须先处理 HTTP 状态码。
- HTTP 2xx 且响应体 `code=200` 才表示业务成功。
- 受保护接口通过 `Authorization: Bearer <token>` 传递登录凭证。
- 每个响应头和响应体中都有可用于排障的 `Trace-Id/traceId`，前端错误日志应一并记录。
- HTTP 401 表示 Token 缺失、失效或账号在其他位置重新登录，前端应清理本地登录状态并跳转登录页。
- HTTP 403 表示账号、身份状态或权限不允许，不能一律当作“未登录”处理。
- HTTP 409 表示账号、邮箱等唯一数据冲突。
- HTTP 429 表示发送频率、登录失败次数或每日次数达到限制，前端应停止自动重试。
- HTTP 5xx 表示服务端异常，前端可以提示稍后重试，但禁止无限循环请求。
- 按钮提交期间应进入 loading 状态并禁止重复点击；网络超时后是否重试，应根据接口是否幂等分别处理。

失败响应示例：

```json
{
  "code": 2010,
  "message": "账号或密码错误",
  "data": null,
  "traceId": "7a5f9c0c2f2245ef83a337dcd54e7ce4"
}
```

前端建议建立统一 HTTP 拦截器：

```typescript
api.interceptors.request.use((config) => {
  const token = authStore.token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (body.code !== 200) {
      return Promise.reject(new BusinessError(body.code, body.message, body.traceId));
    }
    return body.data;
  },
  (error) => {
    if (error.response?.status === 401) {
      authStore.clear();
      router.replace({name: "login", query: {redirect: router.currentRoute.value.fullPath}});
    }
    return Promise.reject(error);
  }
);
```

以上代码是交互示意。实际项目应在 HTTP 错误分支中继续解析后端统一错误响应，否则会丢失业务码和 TraceId。

### 用户注册调用链

注册分为“发送邮箱验证码”和“提交注册”两步。注册成功后不会自动登录，前端应跳转登录页或主动调用登录接口。

```text
注册页面
  │
  ├─ 1. POST /user/send-captcha
  │      │
  │      ├─ 校验邮箱格式和是否已注册
  │      ├─ Redis 原子占用发送窗口
  │      ├─ 检查邮箱当日发送次数
  │      ├─ 发送六位验证码邮件
  │      └─ Redis 保存验证码和有效期
  │
  └─ 2. POST /user/register
         │
         ├─ Redis 原子校验并预占验证码
         ├─ 规范化账号、邮箱、昵称和大陆手机号
         ├─ 校验账号和邮箱唯一性
         ├─ 创建 User 统一主体
         ├─ 创建 password 类型 UserIdentity
         ├─ 分配 business:member 默认角色
         ├─ 提交数据库事务
         └─ 事务提交后删除验证码；回滚时恢复验证码
```

#### 第一步：发送注册验证码

```http
POST /user/send-captcha
Content-Type: application/json

{
  "email": "user@example.com"
}
```

成功响应：

```json
{
  "code": 200,
  "message": "成功",
  "data": null,
  "traceId": "..."
}
```

当前开发配置下，验证码有效期为 5 分钟，同一邮箱每天最多发送 3 次；具体值由 `mail.register-time` 和 `mail.register-max` 控制。

前端交互要求：

- 邮箱通过前端基础格式校验后才能启用“发送验证码”按钮，但后端校验仍是最终边界。
- 请求成功后启动与 `mail.register-time` 一致的倒计时，并临时禁用重复发送。
- 倒计时只用于用户体验，刷新页面后是否允许重发仍以后端 Redis 状态为准。
- 收到业务码 `8001` 时提示用户检查收件箱和垃圾邮件，不要自动循环发送。
- 收到 `8005` 时提示邮箱已注册，并提供跳转登录或找回密码入口。
- 收到 HTTP 429/业务码 `2009` 时停止当天重试。
- 网络超时不代表邮件一定未发送；前端不能在超时后立即高频重试。

#### 第二步：提交注册

```http
POST /user/register
Content-Type: application/json

{
  "loginCode": "demo-user",
  "loginName": "示例用户",
  "password": "Framework2026",
  "linkPhone": "13800138000",
  "email": "user@example.com",
  "captcha": "123456"
}
```

字段约束：

| 字段 | 约束 |
| --- | --- |
| `loginCode` | 必填，4～50 位；服务端会统一去除首尾空格并规范化 |
| `loginName` | 必填，2～50 位 |
| `password` | 必填，满足 `security.password-policy` |
| `linkPhone` | 可选；非空时必须为中国大陆手机号 |
| `email` | 必填，合法邮箱，最长 100 位 |
| `captcha` | 必填，必须与该邮箱当前有效验证码一致 |

前端交互要求：

- 注册按钮提交期间必须禁用，避免相同表单被重复发送。
- 不要在日志、埋点、URL 查询参数或错误上报中记录密码和验证码。
- 业务码 `8002` 表示验证码不存在或已过期，应引导用户重新发送。
- 业务码 `2007` 表示验证码不匹配，应保留其他表单字段让用户重新输入。
- HTTP 409/业务码 `2003` 或 `8005` 表示账号或邮箱已被占用，应定位到对应输入框提示。
- 注册事务失败时验证码会恢复，用户可以再次提交；注册成功后验证码立即失效。
- 注册成功后清除密码和验证码字段，再跳转登录页。不要默认认为注册响应中包含 Token。

### 密码登录调用链

```text
登录页面
  │
  └─ POST /auth/login
         │
         ├─ 规范化登录账号并解析可信客户端 IP
         ├─ Redis 按账号和 IP 原子预占登录尝试
         ├─ 查询 password 类型 UserIdentity
         ├─ BCrypt 校验明文密码与数据库摘要
         ├─ 校验身份 verified/status 和 User status
         ├─ 加载角色、权限快照
         ├─ 构造 LoginPrincipal
         ├─ Sa-Token 建立账号 Session 并写入 Redis
         ├─ 返回 Token
         └─ 异步更新设备、IP 归属地并检查异常登录
```

请求：

```http
POST /auth/login
Content-Type: application/json

{
  "loginCode": "demo-user",
  "password": "Framework2026"
}
```

成功响应：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "token": "0f85a061-4a8b-4b16-bb6d-47d667cb96f7"
  },
  "traceId": "..."
}
```

前端收到 Token 后：

1. 保存当前登录态。
2. 后续请求增加 `Authorization: Bearer <token>`。
3. 调用 `GET /user/info` 获取当前主体公开资料、角色和后端权限。
4. 调用 `GET /user/menus` 获取当前角色可见菜单树。
5. 根据后端权限控制按钮展示；真正的安全校验仍必须由后端完成。
6. 登录成功后跳转到登录前保存的 `redirect`，没有 redirect 时进入默认首页。

Token 存储建议：

- 当前后端契约是“响应 JSON 返回 Token，并通过 Authorization Header 传递”，前端不能假设服务端写入了 HttpOnly Cookie。
- 浏览器端优先保存在应用内存状态；如果必须跨刷新保存，应充分评估 localStorage/sessionStorage 的 XSS 风险并落实 CSP、依赖治理和输出转义。
- 如果项目要求 HttpOnly、Secure、SameSite Cookie，需要前后端一起调整登录、跨域、CSRF 和注销设计，不能只由前端自行改存储方式。
- 禁止把 Token 放入 URL、页面埋点、前端异常日志或第三方统计参数。

登录错误处理：

| HTTP/业务码 | 前端行为 |
| --- | --- |
| `401 / 2010` | 统一提示账号或密码错误，不区分账号是否存在 |
| `429 / 2012` | 提示尝试次数过多并停止自动提交，等待统计窗口结束 |
| `403 / 2011` | 提示账号已停用，并提供联系管理员入口 |
| `403 / 2013` | 提示当前登录身份已停用 |
| `403 / 2014` | 提示当前登录身份尚未验证 |
| `400 / 1001` | 根据 `message` 提示参数格式问题 |

当前配置 `sa-token.is-concurrent=false`，同一账号新登录会使旧 Token 失效。因此其他设备收到 401 时，应退出旧会话并提示“账号可能已在其他设备重新登录”，不要在后台无限刷新 Token。

管理端密码登录使用管理端地址的 `POST /auth/login`，请求字段相同，但 Token 属于独立 `admin` 安全域。前端如果同时提供用户端和管理端页面，必须使用不同的状态容器和 API Client，不能共用 Token。

#### 支付宝登录补充

支付宝小程序登录接口：

```http
POST /ali-auth/login
Content-Type: application/json

{
  "authCode": "支付宝客户端取得的一次性授权码"
}
```

前端只能提交支付宝返回的一次性 `authCode`，不得提交或伪造支付宝 userId。后端负责使用可信应用凭据换取平台身份、绑定统一用户、加载 RBAC 快照并返回与密码登录相同结构的系统 Token。

### 找回密码调用链

找回密码分为“请求邮件”和“通过邮件链接提交新密码”两步。接口会故意隐藏邮箱是否已注册，防止被用于枚举系统用户。

```text
忘记密码页面
  │
  ├─ 1. POST /user/reset-password-email
  │      │
  │      ├─ 规范化邮箱
  │      ├─ 查询邮箱对应的 User
  │      ├─ 不存在时仍返回成功，隐藏账号存在性
  │      ├─ Redis 原子占用发送窗口并检查每日次数
  │      ├─ 创建包含 realm、subjectId、email 的临时 Token
  │      ├─ 邮件发送前生成前端重置页面 URL
  │      └─ 发送成功后保存 Token 窗口状态
  │
  └─ 用户点击邮件链接：reset-password.base-path?token=...
         │
         ├─ 前端从 URL 读取 Token，展示新密码表单
         └─ 2. POST /user/reset-password
                │
                ├─ Redis 按 Token 摘要原子取得唯一消费权
                ├─ 解码并校验 user 安全域、subjectId 和邮箱
                ├─ BCrypt 编码并更新 password 身份凭据
                ├─ 提交数据库事务
                ├─ 标记 Token 已消费并删除原临时 Token
                └─ 注销该用户全部旧会话
```

#### 第一步：请求重置邮件

```http
POST /user/reset-password-email
Content-Type: application/json

{
  "email": "user@example.com"
}
```

无论邮箱是否存在，正常情况下都返回统一成功响应。前端文案应使用：

> 如果该邮箱已注册，系统会发送重置密码邮件，请检查收件箱和垃圾邮件。

不要显示“邮箱不存在”，也不要通过响应耗时、自动跳转或不同倒计时泄露邮箱是否注册。

当前开发配置下，同一邮箱每天最多请求 3 次，重置链接有效期为 15 分钟，分别由 `reset-password.max-number` 和 `reset-password.reset-pwd-time` 控制。

前端交互要求：

- 请求成功后展示统一提示，不自动进入新密码输入页。
- 收到 `8001` 时提示邮件已发送过，不重复发送。
- 收到 HTTP 429/业务码 `3000` 时提示今日次数已达上限。
- 邮件发送失败 `8000` 可以提示稍后重试，并记录 TraceId 供排查。

#### 第二步：邮件链接与重置页面

后端邮件中的链接由以下配置生成：

```yaml
reset-password:
  base-path: https://frontend.example.com/reset-password
```

最终邮件链接类似：

```text
https://frontend.example.com/reset-password?token=temporary-token
```

因此 `reset-password.base-path` 必须配置为前端重置密码页面，而不是后端 API 地址。

重置页面加载时：

1. 从当前 URL 查询参数读取 `token`。
2. Token 缺失时直接提示链接无效，不发送空请求。
3. 不需要也不应该在页面加载时把 Token 发送给第三方统计、客服或埋点 SDK。
4. 不要把 Token 长期写入 localStorage；保留在当前页面内存即可。
5. 用户提交新密码时，将 Token 和新密码一起发送给后端。

```http
POST /user/reset-password
Content-Type: application/json

{
  "token": "temporary-token",
  "newPassword": "NewFramework2026"
}
```

前端交互要求：

- 新密码必须按当前 `security.password-policy` 提供即时提示，但以后端校验结果为准。
- 提交期间禁用按钮。一个 Token 只允许成功消费一次，并发提交只会有一个成功。
- 业务码 `3001` 表示链接过期、已消费、跨安全域或载荷不一致，应引导用户重新发起找回密码。
- 重置成功后立即从地址栏移除 Token，可以使用路由替换而不是保留当前历史记录。
- 重置成功会注销该用户所有旧 Token；前端应清理任何已有登录态并跳转登录页。
- 不要在重置成功后自动使用新密码登录，除非产品明确要求并重新评估安全策略。

管理端找回密码使用管理端地址下相同的：

```text
POST /user/reset-password-email
POST /user/reset-password
```

管理端邮件 Token 包含 `admin` 安全域，不能提交到用户端接口。用户端与管理端前端应分别配置自己的重置页面地址和 API Base URL。

### 修改密码与找回密码的区别

已登录用户修改密码使用：

```http
POST /user/change-password
Authorization: Bearer <token>
Content-Type: application/json

{
  "password": "CurrentPassword",
  "newPassword": "NewFramework2026"
}
```

- 修改密码必须携带当前有效会话并验证旧密码。
- 找回密码不要求登录，但必须持有邮件中的一次性 Token。
- 两种方式成功后都会注销该主体全部旧会话。
- 前端成功后都应清理本地 Token 并跳转登录页。

## 构建、测试与打包

完整验证：

```bash
./mvnw clean verify
```

该命令会执行单元测试和 Testcontainers 集成测试，覆盖：

- MySQL、Redis 和完整 Spring 上下文启动
- business/admin Flyway 从空库迁移及 V1 到 V2 升级
- Mapper XML 实际执行
- Redis Lua 原子行为
- Sa-Token Session 实际读写
- 注册、登录、限流、验证码并发消费
- 支付宝首次登录并发绑定
- 密码修改、重置和旧会话失效
- 用户端及管理端 RBAC 安全不变量
- bootstrap 和共享数据库部署边界

测试环境需要可用的 Docker。普通单元测试禁止连接开发机上的真实 MySQL、Redis 或邮件服务。

只编译不运行测试：

```bash
./mvnw -DskipTests compile
```

打包：

```bash
./mvnw clean package
```

部署时使用可执行 JAR：

```text
business/target/business-1.0.0-exec.jar
admin/target/admin-1.0.0-exec.jar
```

普通 JAR 用于模块依赖和架构测试，不是生产启动入口。

## 自定义注解

项目当前提供六个自定义注解。使用注解时应理解其职责边界，不要把业务逻辑继续塞入切面或拦截器。

### `@ResponseResult`：启用统一响应包装

包路径：

```java
com.yxx.common.annotation.response.ResponseResult
```

可标注在 Controller 类或方法上。只有显式标注的接口才会包装为 `BaseResponse<T>`，该能力不默认全局启用。

```java
@ResponseResult
@RestController
@RequestMapping("/orders")
public class OrderController {

    @GetMapping("/{id}")
    public OrderResp detail(@PathVariable Long id) {
        return orderService.detail(id);
    }
}
```

处理规则：

- 普通返回值包装为成功响应。
- 已经是 `BaseResponse<?>` 时不会二次嵌套，只补充 TraceId。
- `ErrorResponse` 包装为失败响应。
- `String` 返回值会显式序列化为 JSON，避免被 `StringHttpMessageConverter` 当作普通文本输出。

文件下载、流式响应或需要精确控制原始响应体的接口不要使用该注解。

### `@AllowAnonymous`：允许匿名访问

包路径：

```java
com.yxx.security.annotation.AllowAnonymous
```

系统采用“默认需要登录”策略。只有明确标注 `@AllowAnonymous` 的 Controller 类或方法才允许不携带 Token 访问。

```java
@AllowAnonymous
@PostMapping("/login")
public LoginRes login(@Valid @RequestBody LoginReq request) {
    return authenticationService.login(request);
}
```

使用要求：

- 只用于登录、注册、验证码、找回密码、健康检查等确实公开的接口。
- 不要把它标在整个业务 Controller 上图省事。
- 匿名不等于无需参数校验、限流、审计或防重放。

### `@SaAdminCheckPermission`：管理端权限校验

包路径：

```java
com.yxx.security.annotation.SaAdminCheckPermission
```

该注解是指定了管理端 `StpLogic` 的 `@SaCheckPermission` 组合注解，避免误用默认用户端安全域。

```java
@SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_AUDIT_LOG_READ)
@GetMapping("/page")
public PageResponse<OperateLogResp> page(OperateLogReq request) {
    return operateAdminLogService.operationLogPage(request);
}
```

多个权限默认使用 AND：

```java
@SaAdminCheckPermission({"order:read", "order:export"})
```

需要满足任意一个权限时：

```java
@SaAdminCheckPermission(
        value = {"order:read", "order:review"},
        mode = SaMode.OR)
```

权限编码应定义为安全常量并使用冒号分层，例如 `order:read`、`order:refund:approve`，禁止在各 Controller 中散落手写字符串。

用户端权限校验可以使用 Sa-Token 原生 `@SaCheckPermission`；管理端必须使用 `@SaAdminCheckPermission` 或显式指定 `type=admin`。

### `@AuditLog`：声明式操作审计

包路径：

```java
com.yxx.framework.audit.annotation.AuditLog
```

示例：

```java
@AuditLog(
        module = "订单模块",
        action = "取消订单",
        eventType = AuditEventType.OPERATION,
        resource = "order")
@PostMapping("/{id}/cancel")
public void cancel(@PathVariable Long id) {
    orderService.cancel(id);
}
```

属性说明：

| 属性 | 必填 | 说明 |
| --- | --- | --- |
| `module` | 是 | 业务模块名称，用于日志展示和查询 |
| `action` | 是 | 结构化操作名称，例如“取消订单” |
| `eventType` | 否 | `AUTHENTICATION`、`OPERATION` 或 `SECURITY`，默认 `OPERATION` |
| `resource` | 否 | 资源类型或说明，例如 `order` |
| `recordRequest` | 否 | 是否记录脱敏后的请求参数，默认 `true` |
| `subjectField` | 否 | 匿名请求中只提取指定账号字段，不记录整个请求对象 |

登录、修改密码、重置密码、验证码等敏感接口必须关闭请求参数记录：

```java
@AuditLog(
        module = "鉴权模块",
        action = "用户密码登录",
        eventType = AuditEventType.AUTHENTICATION,
        recordRequest = false,
        subjectField = "loginCode")
```

审计切面只采集上下文并发布 `AuditEvent`。数据库存储由具体应用的事件监听器负责，因此新增业务应用时可以建立自己的审计表和监听器，而不需要修改公共切面。

审计是辅助链路。审计发布或异步持久化失败会记录服务端错误，但不会覆盖原业务方法的成功或异常结果。

### `@Password`：配置化密码策略校验

包路径：

```java
com.yxx.security.validation.Password
```

新密码使用完整策略：

```java
@NotBlank(message = "新密码不能为空")
@Password
private String newPassword;
```

登录密码和旧密码不能套用当前完整策略，因为用户密码可能创建于策略升级之前；此时只校验 BCrypt 最大字节限制：

```java
@NotBlank(message = "密码不能为空")
@Password(enforcePolicy = false, message = "密码长度超过系统限制")
private String password;
```

密码规则由 `security.password-policy` 配置：

```yaml
security:
  password-policy:
    min-length: 12
    max-bytes: 72
    require-uppercase: true
    require-lowercase: true
    require-digit: true
    require-special-character: false
    allow-whitespace: false
```

`@Password` 不负责空值校验，字段仍应配合 `@NotBlank` 使用。`max-bytes` 按 UTF-8 字节计算，因为中文和 Emoji 的字符数与 BCrypt 接收的字节数并不相同。

### `@QueryDateBoundary`：查询日期边界转换

包路径：

```java
com.yxx.common.annotation.jackson.QueryDateBoundary
```

用于把查询请求中的日期转换为当天开始或结束时间：

```java
@QueryDateBoundary(QueryDateBoundary.Boundary.START_OF_DAY)
private Date startTime;

@QueryDateBoundary(QueryDateBoundary.Boundary.END_OF_DAY)
private Date endTime;
```

例如输入 `2026-07-16` 后：

- `START_OF_DAY` 转为当天 `00:00:00.000`。
- `END_OF_DAY` 转为当天结束时间。

该注解只用于 Jackson 反序列化的 `Date` 字段，适合请求体中的查询条件。URL 查询参数由 Spring 参数绑定处理，不会自动经过 Jackson 反序列化器；如需支持 URL 参数，应单独增加 Converter 或在服务层规范化。

## 认证与授权设计

### 用户端统一认证模型

用户端采用：

```text
User（统一主体） 1 ---- N UserIdentity（登录身份）
```

密码、支付宝以及未来的微信、企业微信等身份都必须先映射为稳定的内部 `userId`，再由统一认证编排服务完成：

1. 身份认证。
2. 登录风险处理。
3. 加载角色和权限快照。
4. 构造 `LoginPrincipal`。
5. 建立 Sa-Token Session。

`LoginPrincipal` 是运行时安全快照，不是数据库实体，也不应直接作为接口响应模型使用。

### 管理面、业务面与统一 RBAC

- 用户端使用 Sa-Token 默认安全域。
- 管理端使用独立的 `admin` 安全域。
- 两个应用共用 `rbac_role`、`rbac_permission`、`rbac_menu` 及三张关联表。
- `scope=admin` 表示管理后台权限域，`scope=business` 表示业务用户权限域。
- `subject_type=admin` 只能关联 admin 角色，`subject_type=user` 只能关联 business 角色。
- Java 服务校验、数据库 `CHECK` 约束和复合外键共同阻止跨权限域授权。
- admin 可以管理两个权限域；business 只读取业务用户的 business 授权结果。
- 相同数据库 ID 在两个安全域内没有任何身份关联。
- 权限或角色变化后，必须在事务提交后注销受影响主体的全部会话，使旧权限快照立即失效。

`common-security` 中的 `AuthorizationProvider` 是稳定授权抽象，`common-rbac` 提供默认
数据库实现。后续接入 LDAP、IAM 或远程权限中心时，可以替换授权实现，而不需要修改登录
编排和 Sa-Token 会话代码。

### 管理端管理业务用户

管理端提供以下基础接口：

```text
GET /management/business-users
GET /management/business-users/{userId}
PUT /management/business-users/{userId}/roles

GET /management/rbac/roles?scope=admin|business
GET /management/rbac/permissions?scope=admin|business
GET /management/rbac/menus?scope=admin|business
PUT /management/rbac/roles/{roleId}/permissions
PUT /management/rbac/roles/{roleId}/menus
```

业务用户没有直接修改角色的接口。前端管理系统先读取 `business` 权限域角色，再把最终
角色主键集合提交给业务用户角色接口。后端会验证目标用户存在、角色全部属于 business
权限域，并在事务提交后注销该用户旧会话。

### 角色与权限编码

角色和权限编码统一使用冒号分层：

```text
business:member
business:operator
admin:administrator
admin:super-admin
business:order:read
business:order:create
admin:order:refund:approve
```

编码必须集中定义在安全常量类中，并同步维护 Flyway 初始化数据。菜单编码用于前端导航，后端接口权限使用独立权限表，二者不要复用同一字段表达不同语义。

## 标准扩展方式

### 新增一种用户登录方式

以微信登录为例：

1. 在 `LoginMode` 中增加稳定编码，例如 `wechat`。
2. 新增实现 `UserAuthenticationCommand` 的命令类型。
3. 新增实现 `UserAuthenticationStrategy` 的 Spring Bean。
4. 策略负责验证平台凭据，并返回 `AuthenticatedUser`。
5. 首次登录绑定逻辑放入独立事务服务，不要在外部网络调用期间占用数据库事务。
6. 在 `user_identity` 中增加对应身份记录，不要给 `User` 实体增加 `wechatOpenId` 等平台专属字段。
7. 增加首次登录、已绑定登录、停用身份和并发首次登录集成测试。

策略骨架：

```java
@Component
@RequiredArgsConstructor
public class WechatAuthenticationStrategy implements UserAuthenticationStrategy {

    @Override
    public String loginMode() {
        return LoginMode.WECHAT;
    }

    @Override
    public AuthenticatedUser authenticate(UserAuthenticationCommand command) {
        // 1. 校验命令类型。
        // 2. 使用服务端可信凭据向微信换取平台身份。
        // 3. 查询或创建统一用户及 UserIdentity。
        // 4. 校验身份和用户状态。
        // 5. 返回统一认证结果，不在此处自行创建 Sa-Token Session。
        throw new UnsupportedOperationException("请按项目认证流程实现微信登录");
    }
}
```

不要修改 `UserAuthenticationService` 增加 `if/else`。Spring 会自动收集新策略，并在启动时校验登录模式是否重复。

### 新增业务模块

建议为订单、库存等可独立演进的领域建立新的 Maven 模块，而不是继续扩大 `business`：

```text
order
├── controller
├── service
├── domain 或 model
├── mapper
└── test
```

标准步骤：

1. 在根 `pom.xml` 注册模块。
2. 依赖 `common-framework`，不要逐个重复拼装所有公共模块。
3. 为应用设置唯一 `app.name` 和端口；仍然连接当前共享 Schema。
4. 建立本领域自己的实体、DTO、Mapper、错误码和安全常量。
5. 将表结构变化追加到 `database-migrations` 的下一版本迁移，不允许应用私建历史表。
6. 如果它是新的独立登录安全域，增加对应 `StpLogic` 和 `CurrentActorProvider` 适配；如果只是用户端下属业务，则复用用户身份。
7. 增加空库迁移、应用启动、Mapper XML 和核心业务集成测试。

只有确定与领域无关、至少被多个应用复用的能力，才考虑拆到新的 `common-*` 模块。

### 新增角色、权限和菜单

1. 在对应应用的安全常量类中增加权限编码；公共内置角色编码放在 `RbacSecurityCodes`。
2. 新增共享 Flyway 迁移，写入正确 `scope` 的角色、权限、菜单和必要关联。
3. Controller 使用对应安全域的权限注解。
4. 菜单只负责导航，接口权限只使用权限码。
5. 运行期调整必须通过 admin 管理接口或公共替换服务，以确保权限域校验和会话失效。
6. 内置超级管理员角色、权限通配符和“至少一个启用超级管理员”属于安全不变量，不得绕开服务直接修改关联表。

菜单约定：

- `status=false`：菜单不可用，不进入菜单树。
- `visible=false`：只是不展示当前节点，不代表其可见子菜单也不可用。
- 隐藏父菜单下的可见子菜单会提升到最近的可见祖先；没有可见祖先时提升为根节点。
- 菜单树构建器会防御循环父子关系，但数据库迁移和管理接口仍应阻止产生循环数据。

### 扩展审计存储

公共审计模块通过 Spring Application Event 发布 `AuditEvent`。新应用可以增加自己的监听器：

```java
@Service
public class OrderAuditEventListener {

    @Async("auditTaskExecutor")
    @EventListener
    public void save(AuditEvent event) {
        // 将通用事件转换为本应用审计实体并持久化。
        // 异步异常必须在监听器内记录，不能依赖调用方事务回滚。
    }
}
```

如需投递消息队列，可新增 `AuditEventPublisher` 实现或事件监听器，但应继续保持业务方法与具体存储技术解耦，并明确消息可靠性、失败告警和幂等策略。

### 扩展 Redis 能力

- Redis Key 前缀集中维护，禁止在业务代码中散落字符串。
- 涉及“检查后修改”、计数器、一次性消费的逻辑必须使用 Redis 原子命令或 Lua。
- Redis Key 不直接放密码、Token、邮箱重置令牌等敏感原文，应使用摘要。
- 所有临时数据必须有 TTL；配置异常时宁可使用有限默认值，也不要创建永久安全状态。
- 不要自行创建新的 `RedissonClient`，连接生命周期由 Starter 和 Spring 容器统一管理。

### 扩展配置项

新增可配置能力时：

1. 使用 `@ConfigurationProperties` 建立类型安全配置类。
2. 提供安全且适合基础框架的默认值。
3. 对 IDE 无法自动识别的属性维护 `additional-spring-configuration-metadata.json`。
4. 敏感配置通过环境变量或密钥管理系统注入，不写入源码和生产配置模板。
5. 为默认值、边界值和 Bean 注册唯一性增加测试。

### 扩展异步任务

- 普通辅助任务使用 `applicationTaskExecutor`。
- 审计持久化使用 `auditTaskExecutor`。
- 禁止使用 `CompletableFuture` 默认公共线程池。
- 新增高吞吐、可独立积压的任务类型时，应建立独立有界线程池或消息队列。
- 异步线程不能直接读取 `HttpServletRequest`；必须在请求线程内提取必要数据。
- 需要链路追踪时应保留现有 MDC 传播和任务结束清理机制。

## 基础设施约定

### 统一响应与异常

- `@ResponseResult` 为显式启用，不默认包装全部 Controller。
- 业务异常使用 `ApiException` 和 `ApiCode`。
- HTTP 状态码负责表达通用协议语义，业务错误码负责表达具体业务场景。
- 未知异常只向客户端返回通用消息，完整堆栈保留在服务端日志。
- Controller 不直接返回 MyBatis-Plus `Page`，统一转换为 `PageResponse<T>`。

### 认证与密码

- 注册、登录、修改密码和重置密码统一使用同一个 `PasswordEncoder`。
- `PasswordEncoder.matches` 参数顺序是“请求明文、数据库摘要”，禁止直接用字符串比较密码。
- 登录账号和邮箱在查询、缓存 Key 生成和持久化前统一规范化。
- 登录身份必须同时满足 `status=true` 和 `verified=true`。
- 新密码使用配置化密码策略；旧密码和登录密码只校验编码器安全上限。
- 密码修改和重置成功后，在事务提交后注销该主体的全部旧会话。
- 一次性验证码和重置 Token 支持并发唯一消费及事务失败重试。

### TraceId、访问日志与审计

- 每个 HTTP 请求都会生成或继承合法的 `Trace-Id`，并通过响应头返回。
- 请求结束后清理 ThreadLocal 和 MDC，防止容器线程复用导致串号。
- 密码、Token、授权头、密钥和签名进入日志前统一脱敏。
- Controller 访问日志默认关闭，可通过 `WEB_ACCESS_LOG_ENABLED=true` 开启。
- 业务审计由显式 `@AuditLog` 控制，与访问日志开关相互独立。
- 审计日志保存事件发生时的主体快照，历史查询不依赖当前用户资料。

### 数据与缓存

- 分页大小上限为 200，MyBatis-Plus 拦截器执行最终限制。
- 禁止无条件全表更新和删除。
- Redis 连接由 Starter 管理，禁止业务代码自行维护第二套连接池。
- 登录失败通过账号和 IP 双维度 Redis Lua 原子预占。
- 密码重置 Token 使用摘要作为 Redis Key，事务提交后保留已消费标记至原 Token 过期。

### IP 与代理

- 只有请求直接来源位于 `ip.trusted-proxies` 时才信任 `X-Forwarded-For` 和 `X-Real-IP`。
- 生产环境必须按真实网关地址配置可信代理，不能使用不受控的全网段通配。
- IP 归属地和异常登录邮件属于辅助能力，解析失败不能阻断认证和业务请求。
- `common-ip/src/main/resources/ip2region/ip2region.xdb` 是运行时需要的归属地数据库资源，不应删除。

### 接口路径

- 路径统一使用 kebab-case，例如 `/send-captcha`、`/reset-password`、`/change-password`。
- 当前项目仍处于架构阶段，不保留旧 camelCase 路径兼容入口。

## 数据库迁移规范

唯一迁移目录：

- `database-migrations/src/main/resources/db/migration/shared`

管理端和业务端共同使用 `flyway_schema_history`。禁止在应用模块中再次创建独立迁移目录或
历史表，否则会重新引入启动顺序和跨模块外键无法统一管理的问题。

已经发布或提交到共享分支的迁移文件禁止修改。表、字段、索引、约束和初始化数据变化必须新增更高版本迁移，例如：

```text
V3__add_user_profile.sql
V4__add_order_permission.sql
```

SQL 规范：

- 表和字段必须提供中文 `COMMENT`。
- 明确主键、唯一约束、外键策略和必要索引。
- RBAC 初始化数据的编码必须与 Java 安全常量保持一致。
- 迁移应同时验证从空库完整执行和从上一版本升级。
- 禁止继续维护可重复执行的全量建表脚本代替 Flyway。

详细说明见 [`db/README.md`](db/README.md)。

## 开发检查清单

提交代码前至少确认：

- 新接口是否明确选择 `@ResponseResult`、登录要求、权限要求和审计要求。
- 匿名接口是否仍有参数校验、限流、防重放和敏感信息保护。
- DTO、实体、Session 主体和第三方平台模型是否保持边界清晰。
- 权限变化是否使旧会话失效。
- Redis 并发逻辑是否真正原子，并且所有临时 Key 都有 TTL。
- 数据库变化是否通过新 Flyway 迁移实现，并包含中文注释。
- 异步任务是否使用有界执行器并正确传播、清理 MDC。
- 核心业务是否有单元测试；涉及数据库、Redis、Mapper、事务和并发时是否有集成测试。
- `./mvnw clean verify` 是否完整通过。
