# 认证、Session 与 RBAC

## 安全域映射

应用域、Sa-Token `loginType`、主体类型和 RBAC scope 是四个不同概念，必须按下表映射，不能因当前字符串恰好相同而混用：

| 应用入口 | application realm（`app.name`） | Sa-Token loginType | Session 读取路径 | 认证主体 subjectType / actorType | RBAC scope |
|---|---|---|---|---|---|
| business | `user` | `StpUtil.TYPE` | `StpUtil` | `user` | `business` |
| admin | `admin` | `StpAdminUtil.TYPE` | `StpAdminUtil` | `admin` | `admin` |

- `StpUtil.TYPE` 是 Sa-Token 默认账号体系标识，不假设它等于 application realm `user`。
- 公共认证、授权、Session 属性名和会话服务可以复用，但必须显式接收或解析安全域；Token 与 loginId Session 的存储命名空间、注销逻辑和当前主体读取路径必须按账号体系隔离。
- 当前 actor 必须由正在运行的 application realm 选择对应 Session，不能通过“先查 admin、再查 user”等命中顺序猜测。
- 未知 application realm、loginType 或 subjectType 必须拒绝或返回无主体，不能静默降级到用户端。
- 完整 `LoginPrincipal` 快照存入 loginId Session；角色和权限以数据库稳定主体 ID 为归属，不使用账号、邮箱、手机号或第三方用户编号关联。

## 登录 Wire Contract

admin 和 business 的密码登录入口均为 `/auth/login`。稳定响应契约为统一响应外壳中的 `data.token`：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "token": "raw-token-value"
  },
  "traceId": "..."
}
```

客户端从统一响应的 `$.data.token` 取得原始 Token，后续请求使用：

```http
Authorization: Bearer <raw-token-value>
```

变更登录路径、Token 字段、承载位置或认证 Header 均属于公共契约变更，必须提供兼容迁移方案并同步客户端与集成测试。

## 登录与凭据

- 认证失败对“不存在账号”和“密码错误”使用相同外部响应，防止账号枚举；密码正确后才允许暴露停用、未验证等账号状态。
- BCrypt 校验前执行账号/IP 双维度登录保护，避免高并发请求绕过频控；成功后清理或更新保护状态。
- 登录成功前完成主体与授权快照装配；风险通知、IP 归属地等辅助链路失败不能破坏已建立的认证结果。
- 新密码执行当前应用的 `security.password-policy` 配置，并始终遵守 BCrypt 72 UTF-8 字节上限。admin 与 business 独立部署，可以配置不同策略；不要把一端的运行值当成全局常量。
- 登录密码和旧密码校验只要求非空及编码器安全上限，不能因策略升级阻止存量用户验证旧密码；新密码、重置密码和 bootstrap 初始密码执行完整策略。
- 验证码和重置 Token 使用原子的一次性消费；事务回滚时按临时凭据服务契约恢复或保留可重试状态，成功后不得再次使用。
- 密码、验证码、重置 Token、授权头和完整第三方凭据不进入访问日志或审计参数。

## RBAC 与会话失效

- admin、business 共用 RBAC 表，但主体类型、角色 scope、权限 scope 和关联表 scope 必须与映射表一致；数据库复合约束和代码校验共同防护。
- 菜单只负责前端导航，不作为后端接口授权依据；超级管理员能力由内置超级角色和代码通配符规则提供。
- 停用、删除管理员或移除超级角色时，至少保留一名启用的超级管理员。
- 修改或重置密码、账号状态、主体角色关系、角色权限关系、权限启停/删除或角色删除后，使对应安全域的旧授权 Session 失效；角色名称、备注等非授权元数据变化不触发注销。
- 存在数据库事务时，按“安全域 + 主体 ID”合并失效请求，只在事务成功提交后首次注销；事务回滚不注销。没有事务时立即首次注销。
- 首次注销失败后按应用配置执行有限异步重试；重试耗尽必须记录结构化错误并发布可监控事件。该补偿发生在业务提交后，不能回滚已提交事务，也不能宣称旧 Session 已瞬时失效。

## 修改时必须验证

- 映射测试覆盖两个 application realm 到 loginType、Session 读取路径、subjectType 和 RBAC scope 的完整对应关系；未知值被拒绝。
- business Token 不能访问 admin，admin Token 不能访问 business；两个账号体系的 loginId Session 和权限快照互不可读。
- actor 按当前 application realm 解析，即使测试环境同时存在两个账号体系的 Session 也不会串域。
- 登录响应 `$.data.token` 非空，`Authorization: Bearer ...` 可访问对应受保护接口。
- 不存在账号与密码错误的外部响应一致；新密码按各应用配置验证，超过 BCrypt 字节上限始终拒绝。
- 密码修改/重置、账号状态和角色权限变化后旧 Token 无效；事务回滚时旧 Token 保留。
- 会话失效测试覆盖提交后执行、同事务去重、首次失败后有限重试、调度失败和重试耗尽事件。
- 跨 scope 主体、角色、权限和菜单关联在服务层与真实数据库约束中均被拒绝。
