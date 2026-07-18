# 架构与模块边界

## 事实入口

根 `pom.xml`、相关模块 `pom.xml` 和源码是当前依赖与职责的事实来源；下表是阅读索引，修改前必须重新核对。

## 当前 POM 事实

`common` 是聚合父模块，不承载生产代码。当前项目内直接依赖如下；表中未列第三方依赖：

| 模块 | 直接依赖的项目内模块 |
|---|---|
| `common-core` | 无 |
| `common-cache` | `common-core` |
| `common-http-client` | 无 |
| `common-security` | `common-core`、`common-cache` |
| `common-web` | `common-core`、`common-security` |
| `common-ip`、`common-mail` | `common-core` |
| `common-data` | `common-core`、`common-security` |
| `common-audit` | `common-core`、`common-security`、`common-web`、`common-ip` |
| `common-rbac` | `common-data` |
| `common-framework` | `common-web`、`common-security`、`common-cache`、`common-mail`、`common-ip`、`common-data`、`common-audit` |
| `database-migrations` | 无 |
| `admin`、`business` | `common-framework`、`common-rbac`、`database-migrations` |
| `architecture-tests` | 测试范围依赖 `admin`、`business`、`database-migrations` |

`common-http-client` 当前不在 `common-framework` 中，由真实存在出站 HTTP 调用的模块显式选择。专项边界见 [http-client.md](http-client.md)。

## 目标边界

- `common-core` 只承载无 Web、数据库、缓存、外部服务和业务领域语义的基础契约、校验、异常与纯工具。
- 认证、Web、数据、缓存、审计、RBAC、邮件、IP 和 HTTP 客户端能力归属对应 common 模块；能力模块只暴露稳定且最小的公共接口。
- `common-framework` 是现有基础设施聚合入口，不继续吸收可归属具体模块的实现，也不作为所有新增模块的默认依赖。
- `admin` 与 `business` 不互相依赖；common 模块不依赖应用模块；`database-migrations` 不依赖应用代码。
- `architecture-tests` 只能在测试范围装配应用，任何生产模块不得反向依赖它。
- 跨应用共享契约使用最小模型或事件，不共享具体业务 Entity、Controller 或 Service。
- 源码直接使用的项目内模块必须在自身 POM 中直接声明，不依赖传递依赖维持编译。

当前 `common-core` 的全局 `ApiCode` 含有用户、RBAC、邮件和支付等领域错误码，属于待治理的实现漂移。不得继续向其中添加领域错误码，也不得以该现状证明领域契约应进入 `common-core`。

当前 `common-rbac` 直接使用 `common-core`、`common-security` 类型但 POM 仅声明 `common-data`，属于待治理的传递依赖漂移，不得复制该模式。

## Common 模块职责

- `common-core`：基础类型、通用响应与异常骨架、纯 Java 工具。
- `common-cache`：Redis、Redisson 和通用缓存基础设施。
- `common-http-client`：出站 HTTP 的公共接入能力。
- `common-security`：认证上下文、Sa-Token 适配、密码与 Session 安全能力。
- `common-web`：HTTP 适配、统一响应、异常映射、过滤器和 Web 配置。
- `common-ip`：可信代理客户端 IP 与归属地解析。
- `common-mail`：通用邮件发送能力。
- `common-data`：MyBatis-Plus、分页、字段填充和数据访问基础配置。
- `common-audit`：审计注解、切面、通用事件和发布机制。
- `common-rbac`：角色、权限、菜单和主体授权能力。

## 新代码放置

按顺序判断：

1. 只服务一个应用或业务流程的实现留在 `admin` 或 `business`。
2. 无基础设施依赖且无领域语义的稳定模型或算法才考虑 `common-core`。
3. 明确属于某项公共能力的实现进入对应 common 模块。
4. 仅组合已有能力时，由应用 POM 选择依赖，不新增空壳模块或扩大聚合模块。
5. 设计若迫使低层模块依赖高层业务，改用应用侧实现、最小接口或事件边界。

## 应用分层

- Controller：协议适配、参数校验、权限声明和响应返回。
- Service/用例层：事务、业务规则和跨仓储协调。
- Mapper：持久化，不承载业务决策。
- Entity：数据库映射，不直接作为外部 API 契约。
- Request/Response DTO：接口契约，不承载持久化行为。
- Event：跨模块最小不可变契约，适合时使用 record。
