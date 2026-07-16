# API、审计与日志

## 统一响应

标记 `@ResponseResult` 的 Controller 返回值由 ResponseBodyAdvice 包装：

```json
{
  "code": 200,
  "message": "...",
  "data": {},
  "traceId": "..."
}
```

- 普通返回值进入 `data`。
- 已是 `BaseResponse` 时只补 TraceId，不重复包装。
- 错误响应不携带部分业务数据。
- HTTP 状态码和业务码按现有映射处理，不把所有失败都返回 HTTP 200。

## TraceId

- 每个 HTTP 请求生成或继承合法 `Trace-Id`。
- 同时写入 MDC、响应头和响应体。
- 请求结束后清理 ThreadLocal 和 MDC。
- 审计表保存 trace ID，便于关联服务端文本日志。

## 审计模型

明确区分：

- actor：谁执行操作，包括稳定 ID、主体类型、账号和显示名快照。
- subject：操作了谁或什么资源，包括账号、类型和稳定 ID。
- result type：执行成功或失败。
- event type：AUTHENTICATION、OPERATION、SECURITY。
- module/action/resource：业务模块、结构化动作和资源类型。

actor 和 subject 不是重复字段。匿名注册、验证码和失败登录没有 actor，但应尽量通过显式 SpEL 记录安全的 subjectAccount。

## `@AuditLog` 使用

- `module`、`action` 必须清晰稳定。
- 能明确资源时填写 `resource`。
- 操作具体主体时填写 `subjectType` 和 `subjectId`。
- 匿名认证、注册、验证码场景用 `subjectAccount` 记录账号或邮箱。
- 登录、注册、重置密码、修改密码等敏感请求使用 `recordRequest=false`。
- 不通过自动反射猜测账号字段，避免密码、验证码被意外序列化。
- 认证事件使用 AUTHENTICATION；修改凭据、验证码、权限调整使用 SECURITY；普通管理操作使用 OPERATION。

## Logback

- dev：只输出彩色控制台日志，不写文件。
- integration、bootstrap：只输出纯文本控制台日志。
- prod：输出纯文本控制台并写滚动文件。
- admin、business 文件名取 `spring.application.name`，分别为 `admin.log`、`business.log`。
- 生产通过 `LOG_PATH` 指定绝对目录；默认 `./logs` 相对于 JVM 当前工作目录。
- 文件日志使用 UTF-8、无 ANSI 颜色码，并按时间和大小滚动压缩。
- 不在测试中生成项目目录日志。

修改 Logback 后至少验证一个非生产 Profile 和 prod Profile，检查是否有未引用 appender、字面量 `%n`、颜色码污染文件或错误路径。

## 访问日志和隐私

- Controller 参数访问日志默认关闭，通过 `WEB_ACCESS_LOG_ENABLED=true` 开启。
- 业务审计与访问日志相互独立。
- 请求参数必须脱敏并截断。
- 禁止记录密码、验证码、Token、Cookie、Authorization、密钥、私钥、签名和完整第三方凭据。
