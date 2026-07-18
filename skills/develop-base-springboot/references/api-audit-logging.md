# API、审计与日志

## 统一响应与 Trace 完整链路

标记 `@ResponseResult` 的 Controller 使用稳定统一响应：

```json
{
  "code": 200,
  "message": "...",
  "data": {},
  "traceId": "..."
}
```

- 普通返回值进入 `data`；已有 `BaseResponse` 只补 TraceId；错误响应不携带部分业务数据。
- HTTP 状态码和业务码按现有映射处理，不把所有失败都返回 HTTP 200。字符串返回值也必须序列化为相同 JSON 外壳。
- 请求过滤器仅继承格式合法的 `Trace-Id`，否则生成新值；同一值写入请求上下文、MDC 和响应头。
- 响应 Advice 将当前 TraceId 写入响应体，审计事件保存同一值，Logback 从 MDC 输出同一值。
- 使用 `common-http-client`/Forest 的下游请求从 MDC 传播 TraceId；其他 SDK 或客户端按其协议显式接入。异步任务如需延续链路，必须复制并在结束后清理上下文。
- 请求完成后在 `finally` 中清理 ThreadLocal 和 MDC，避免容器线程复用造成串链。

## 审计语义

- **actor**：实际执行操作的认证主体快照，包含稳定 `actorId`、由 application realm 决定的 `actorType`、事件发生时的 `actorAccount` 和 `actorName`。匿名请求没有 actor 是正常状态。
- **subject**：被操作的人或资源，使用 `subjectType + subjectId` 表达稳定身份；它与 actor 独立，例如管理员停用业务用户时两者必须同时保留。
- **subjectAccount**：登录、注册、验证码、找回密码等尝试中输入的账号、邮箱或其他登录标识快照。它不能代替 actor，也不承载角色码、菜单码等普通资源键，不得从整个请求对象自动猜测。
- `type` 表示执行成功或失败；`eventType` 表示事件业务分类，两者不能混用。
- `module`、`action` 和 `resource` 分别表示稳定业务模块、结构化动作和资源类型，不能依赖中文日志标题反推事件语义。

事件分类固定为：

| eventType | 使用场景 |
|---|---|
| `AUTHENTICATION` | 登录、退出等身份认证事件 |
| `SECURITY` | 修改或重置凭据、验证码、账号安全状态、角色与权限调整 |
| `OPERATION` | 不属于前两类的普通业务和管理操作 |

## `@AuditLog` 使用

- `module`、`action` 必须清晰稳定；能明确资源时填写 `resource`。
- 操作具体主体时填写 `subjectType` 和显式 `subjectId` SpEL；匿名或失败认证使用显式 `subjectAccount` SpEL。创建时尚无稳定 ID 的普通资源使用 `resource` 和明确动作表达，不能借用 `subjectAccount` 保存资源编码。
- 登录、注册、重置/修改密码、验证码和包含第三方凭据的请求使用 `recordRequest=false`。
- SpEL 只提取审计需要的安全标量；解析失败记录配置错误并降级为空，不改变业务结果。
- 审计切面在调用前捕获 actor，在调用后发布不可变事件，保证注销场景仍保留操作者、成功登录场景可获得新主体。

## 持久化与字段治理

- 结构化审计日志可同步记录；应用数据库监听器采用异步 best-effort 持久化，不参与业务事务，也不提供原子性或可靠消息保证。
- 数据库写入失败、执行器拒绝或应用退出都可能造成审计记录缺失；失败必须留下带 TraceId 的服务端错误和可监控信号，但不能回滚或覆盖业务结果。强合规场景应另行设计事务 Outbox 或可靠消息链路。
- 合理为空：匿名请求的全部 actor 字段。
- 按场景可为空：subject、subjectAccount、异常摘要、请求参数、IP 归属地等可选上下文。
- 实际未接入：没有生产者或链路能力却长期为空的字段。先核对注解、事件转换、持久化、查询用途和近期路线图，再补采集或通过迁移删除；不能根据单条记录判断。
- 不新增没有明确写入来源、查询消费者和近期交付计划的预留字段。

## 访问日志与隐私

- Controller 参数访问日志默认关闭，仅通过 `WEB_ACCESS_LOG_ENABLED=true` 开启；访问日志和业务审计相互独立。
- 只记录定位所需的请求元数据、脱敏截断后的参数、结果类型和耗时，不序列化完整响应、Servlet/Session 对象或上传文件内容。
- 敏感字段至少覆盖密码及摘要、验证码（`captcha`、`otp`、`verificationCode`）、Token、`authCode`、Cookie/Set-Cookie、Authorization、secret、credential、密钥、私钥、签名和完整第三方凭据；字段名大小写、连字符、下划线及嵌套对象不能绕过规则。
- 新增登录方式、临时凭据或第三方回调字段时，同步更新结构化字段规则、非 JSON 文本规则和脱敏测试。

## Logback

- dev 只输出彩色控制台；integration、bootstrap 和其他非 prod Profile 只输出纯文本控制台；prod 输出纯文本控制台和滚动文件。
- 文件名来自 `spring.application.name`，生产目录由 `LOG_PATH`/`logging.file.path` 配置；默认相对 JVM 当前工作目录。
- 文件日志使用 UTF-8 且无 ANSI 颜色码，按时间和大小滚动，归档名以 `.gz` 压缩，并设置保留天数与总容量上限。
- 不在测试中生成项目目录日志。

## 修改时必须验证

- 普通值、已有 `BaseResponse`、错误响应和字符串响应均得到正确外壳，响应 Header 与 Body 的 TraceId 一致。
- 端到端覆盖“入站 Header → 请求上下文/MDC → 响应 Header/Body → 审计记录 → 下游 Header”；非法入站值被替换，请求结束后上下文为空。
- 已认证、匿名、成功登录和注销场景分别得到正确 actor；subject 与 actor 可同时存在，subjectAccount 只来自显式安全表达式。
- 三类 eventType 与成功/失败 type 独立落库和查询；审计 SpEL 失败、异步写库失败或执行器拒绝不改变业务结果，并留下 TraceId 可关联的错误。
- 脱敏测试同时覆盖 DTO/嵌套 JSON 与查询串或表单文本，覆盖全部敏感字段命名变体并断言原值未出现；超长内容被截断。
- Logback 至少验证一个非生产 Profile 和 prod，检查未引用 appender、字面量 `%n`、ANSI 颜色码、`.gz` 压缩归档、保留策略和输出路径。
