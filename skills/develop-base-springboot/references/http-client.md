# 出站 HTTP 与 Forest

## 模块边界

- `common-http-client` 只提供跨应用稳定的 Forest 接入、TraceId 透传和安全日志默认策略。
- 该模块当前不依赖其他项目模块，也不由 `common-framework` 聚合；只有真实调用外部 HTTP API 的模块才直接依赖它。
- 第三方 Client 接口、请求响应 DTO、认证签名、业务错误码、限流、熔断、幂等和重试语义归属实际调用方；确有跨应用稳定复用时，建立职责明确的专用集成能力模块。
- `common-http-client` 不扫描、声明或持有具体第三方平台 Client；只有出现真实消费者时，才由调用方选择依赖。

## 配置归属

- Forest 原生的全局连接池、连接与读取超时、后端实现、变量和全局重试默认值使用 `forest.*`；以当前 Forest 版本和实际绑定结果为准。
- `framework.http-client.*` 只管理框架补充的 TraceId 透传和日志安全策略，不复制 Forest 配置模型。
- 第三方基础地址和平台参数由调用方配置；生产值通过 Profile、环境变量或配置中心注入，不写死在 Client 注解或 Java 代码中。
- 账号、Token、密钥、私钥和签名材料通过环境变量或密钥管理系统注入，不提交真实值，也不在异常和诊断输出中复述。

当前框架扩展键及安全默认值：

| 配置键 | 默认值 | 作用 |
|---|---|---|
| `framework.http-client.trace-id-propagation-enabled` | `true` | 是否透传当前 MDC TraceId |
| `framework.http-client.trace-id-header-name` | `Trace-Id` | 下游请求头名称 |
| `framework.http-client.logging.enabled` | `false` | Forest 日志总开关 |
| `framework.http-client.logging.request` | `false` | 请求概要 |
| `framework.http-client.logging.request-headers` | `false` | 请求头 |
| `framework.http-client.logging.request-body` | `false` | 请求体 |
| `framework.http-client.logging.response-status` | `false` | 响应状态 |
| `framework.http-client.logging.response-headers` | `false` | 响应头 |
| `framework.http-client.logging.response-content` | `false` | 响应内容 |

## TraceId

- 默认从当前线程 MDC 的 `Trace-Id` 读取入口已经建立的非空链路标识，并写入配置的下游请求头；客户端本身不重复校验格式。
- HTTP 入口、消息消费和调度任务在各自边界建立并清理 MDC；客户端不生成 TraceId，也不负责延长上下文生命周期。
- MDC 不存在非空 TraceId 时不添加请求头；修改头名称或关闭透传时同时验证配置绑定和真实出站请求。

## 日志与敏感信息

- Forest 请求和响应日志默认全部关闭；排障时仅显式开启已经确认安全的最小字段。
- 请求概要可能包含完整 URL 和查询参数。密码、验证码、Cookie、Authorization、Token、密钥、私钥、签名及完整请求或响应凭据不得进入日志。
- 调用方在开启 Header、Body 或响应内容日志前必须完成字段级审查；无法可靠脱敏时保持关闭。

## 超时与重试

- 每个外部系统显式定义连接超时和读取超时，不依赖偶然的库默认值满足业务时限。
- `forest.max-retry-count` 全局保持 `0`。只有确认幂等且外部系统允许重复调用的方法，才能配置局部重试。
- Forest 1.8 的局部重试使用 `@Retry`，不使用请求方法注解中已废弃的 `retryCount`；`maxRetryCount=2` 表示初始请求外最多再重试两次，总请求数最多为三次。
- 为每个局部重试提供调用方 `RetryWhen`，白名单允许约定的连接失败、读取超时或特定 5xx；Forest 默认条件会按 `response.isError()` 覆盖 4xx，不能直接作为业务重试契约。
- 配置正数退避上限或专用 `ForestRetryer`，并核算连接超时、读取超时、退避和总请求次数的整体时限；当前默认 `BackOffRetryer` 在 `maxRetryInterval=0` 时会形成即时重试。
- 非幂等写请求只有在具备稳定幂等键、上游契约和重复结果验证时才允许重试；认证失败、参数错误等确定性失败不重试。
- 重试策略属于调用方契约，必须明确触发条件、最大尝试次数、退避和最终错误映射，不能隐藏在公共拦截器中。

## MockWebServer 验收

按触及范围使用 MockWebServer 或等价本地协议服务器验证真实网络行为，而非只验证代理 Bean 创建。

公共 Forest 定制覆盖适用项：

- 配置绑定以及 TraceId 存在、缺失、自定义头名和关闭透传的真实出站行为，测试结束后 MDC 已清理。
- 默认日志保持关闭；显式开关只启用指定字段且不会输出测试凭据。

具体第三方 Client 覆盖适用项：

- 请求方法、路径、查询参数、Header 和请求体序列化符合第三方契约。
- 响应反序列化、非成功状态、连接/读取超时和异常映射符合调用方语义。
- 配置重试时，以服务端收到的请求次数和失败类型验证边界，覆盖允许重试、4xx 不重试和最大次数耗尽。

验收必须确认测试报告中用例实际执行（`tests > 0`）且没有跳过；本地服务未收到请求、测试被跳过或只有 Spring 上下文启动成功，都不能证明 Client 契约成立。
