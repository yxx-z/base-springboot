# 认证、Session 与 RBAC

## 双安全域

- business 用户端使用 Sa-Token 默认 `StpUtil`，Realm/loginType 为 user。
- admin 管理端使用独立 `StpAdminUtil`，Realm/loginType 为 admin。
- 禁止在两个安全域之间复用登录状态、Token 逻辑或 Session Key。
- 完整主体快照存入 loginId Session；角色和权限以稳定数据库主体 ID 为归属。

## 登录契约

admin 和 business 密码登录入口均为 `/auth/login`。统一响应中的 Token 位于：

```json
{
  "code": 200,
  "data": {
    "token": "raw-token-value"
  },
  "traceId": "..."
}
```

前端读取 `response.data.data.token`，后续请求使用：

```http
Authorization: Bearer <raw-token-value>
```

不要在没有明确迁移方案时改为 Cookie、响应头或不同字段名。若扩展返回对象，可考虑 `accessToken`、`tokenType`、`expiresIn`，并同步前端契约和集成测试。

## 登录实现约束

- 认证失败对“不存在账号”和“密码错误”使用统一外部响应，防止账号枚举。
- 密码正确后再暴露停用、未验证等状态。
- BCrypt 校验前执行账号/IP 双维度登录保护，避免并发穿透。
- 登录成功后加载授权快照、创建 Session，再异步处理风险通知和登录元数据。
- 风险邮件、IP 归属地等辅助链路失败不能破坏已经完成的认证。

## 密码和临时凭据

- 新密码遵守统一可配置密码策略和 BCrypt 72 字节安全上限。
- 修改密码、重置密码成功后，在事务提交后注销主体全部旧会话。
- 验证码和重置 Token 必须支持一次性原子消费。
- 事务失败时按现有策略恢复或保留临时凭据状态，不能产生可重复使用窗口。
- 密码、验证码和重置 Token 不进入审计参数。

## RBAC

- admin、business 角色和权限共用表，但通过 `scope` 严格隔离。
- 主体类型、角色 scope 和权限 scope 必须一致；数据库复合外键和代码校验共同防护。
- 菜单只负责前端导航，不作为后端接口授权依据。
- 超级管理员能力由内置超级角色和代码通配符规则提供。
- 停用、删除管理员或移除超级角色时，至少保留一名启用的超级管理员。
- 角色或权限变化后使受影响主体的旧授权 Session 立即失效。

## 修改时必须验证

- 用户端和管理端不能串用 Token。
- 登录响应 `$.data.token` 非空。
- `Authorization: Bearer ...` 可以访问对应受保护接口。
- 密码修改/重置后旧 Token 无效。
- 权限或角色变化后旧 Session 无效。
- 跨 scope 角色、权限关联被拒绝。
