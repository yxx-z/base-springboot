# 管理端应用

管理端负责管理员账号、管理端审计日志，以及统一 RBAC 的管理入口。admin 与 business 共用角色、权限、菜单表和同一个 MySQL Schema，数据库结构统一由根工程的 `database-migrations` 模块维护。

## 首次管理员初始化

先创建空数据库，再使用 bootstrap profile 创建首个超级管理员：

```bash
export SPRING_PROFILES_ACTIVE=bootstrap
export BOOTSTRAP_DB_URL='jdbc:mysql://127.0.0.1:3306/base_app?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai'
export BOOTSTRAP_DB_USERNAME='root'
export BOOTSTRAP_DB_PASSWORD='数据库密码'
export BOOTSTRAP_ADMIN_LOGIN_CODE='admin'
export BOOTSTRAP_ADMIN_LOGIN_NAME='系统管理员'
export BOOTSTRAP_ADMIN_EMAIL='admin@example.com'
export BOOTSTRAP_ADMIN_PASSWORD='ChangeMe2026Strong'

java -jar admin-1.0.0-exec.jar
```

bootstrap 模式会：

- 自动执行共享 `database-migrations` 中的完整 Flyway 迁移。
- 仅在 `admin_user` 为空时创建管理员。
- 使用 BCrypt 保存临时密码。
- 自动绑定 `admin:super-admin` 角色。
- 不启动 Web、Redis、邮件和 Sa-Token。
- 完成后关闭进程。

示例密码仅用于说明配置格式，实际部署必须生成独立的高强度临时密码，禁止直接复用。初始化后应使用正常 Profile 启动管理端，并立即修改临时密码。已有管理员时再次执行会直接失败。
