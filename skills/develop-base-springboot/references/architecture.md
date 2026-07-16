# 架构与模块边界

## 事实来源

修改前读取根 `pom.xml`、相关模块 `pom.xml`、README 的模块结构和目标源码。本文只记录稳定边界，不替代当前依赖声明。

## 顶层模块

- `common`：公共能力聚合父模块，本身不承载业务代码。
- `database-migrations`：admin、business 共用的 Flyway 迁移制品。
- `business`：业务用户端应用和具体业务用例。
- `admin`：管理端应用、管理员账号和统一 RBAC 管理入口。
- `architecture-tests`：跨应用数据库、启动模式和部署边界测试；只能在测试范围依赖应用模块。

## Common 模块职责

- `common-core`：无 Web、数据库和业务语义的基础类型、校验、异常、响应模型与纯工具。
- `common-cache`：Redis/Redisson 接入和通用缓存能力。
- `common-mq`：RabbitMQ JSON 转换和可靠发布能力；业务拓扑、消费者和消息契约留在具体业务模块。
- `common-security`：认证上下文、Sa-Token 双 Realm、密码策略、Session、登录保护和安全注解。
- `common-web`：HTTP、统一响应、异常映射、过滤器、Web 配置和请求日志。
- `common-ip`：可信代理客户端 IP 解析和 IP 归属地能力。
- `common-mail`：邮件发送、邮件功能校验及通用邮件服务。
- `common-data`：MyBatis-Plus、分页、字段填充和数据访问基础配置。
- `common-audit`：审计注解、切面、通用事件及发布机制，不依赖应用审计表实体。
- `common-rbac`：统一角色、权限、菜单和主体授权服务。
- `common-framework`：现阶段的应用公共能力聚合入口。把它视为便利聚合依赖，不在其中继续堆放可以归属具体模块的实现。

## 依赖方向

遵守以下方向：

```text
common-core
   ↑
具体 common 能力模块
   ↑
common-framework（聚合入口）
   ↑
admin / business
   ↑ test scope only
architecture-tests
```

补充约束：

- admin 与 business 不互相依赖。
- common 模块不依赖 admin/business。
- `database-migrations` 不依赖应用代码。
- `architecture-tests` 不得被生产模块反向依赖。
- 跨应用共享契约使用最小模型或事件，不共享具体业务 Entity、Controller 或 Service 实现。

## 新代码放置决策

按顺序判断：

1. 是否只服务一个应用或业务流程？留在 admin/business。
2. 是否是无基础设施依赖的稳定通用模型或算法？考虑 common-core。
3. 是否明确属于缓存、消息队列、安全、Web、IP、邮件、数据、审计、RBAC？进入对应模块。
4. 是否只是组合已有能力？优先由应用 POM 选择依赖，不新增空壳模块。
5. 是否会迫使低层模块依赖高层业务？停止并重新设计接口或事件边界。

## 分层约定

- Controller：协议适配、参数校验、权限注解和响应返回。
- Service/用例层：事务、业务规则、跨仓储协调。
- Mapper：持久化，不承载业务决策。
- Entity：数据库映射，不直接作为外部 API 契约。
- Request/Response DTO：接口契约，不承载持久化行为。
- Event：跨模块最小不可变契约，优先使用 record。

新增业务模块时，直接选择需要的职责模块。不要默认复制 admin/business，也不要为了方便依赖所有基础设施。

RabbitMQ 属于可选基础设施，`common-mq` 不加入 `common-framework`。只有实际生产或消费消息的
模块才直接依赖它；交换机、队列、路由键、死信和重试策略不得带入 common 层业务语义。
