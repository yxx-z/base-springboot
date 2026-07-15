# 管理端应用

管理端拥有独立的管理员账号、角色、权限、菜单、审计日志和 Flyway 迁移。

## 首次管理员初始化

先创建空数据库，再使用 bootstrap profile 创建首个超级管理员：

```bash
export SPRING_PROFILES_ACTIVE=bootstrap
export BOOTSTRAP_DB_URL='jdbc:mysql://127.0.0.1:3306/base_admin?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
export BOOTSTRAP_DB_USERNAME='root'
export BOOTSTRAP_DB_PASSWORD='数据库密码'
export BOOTSTRAP_ADMIN_LOGIN_CODE='admin'
export BOOTSTRAP_ADMIN_LOGIN_NAME='系统管理员'
export BOOTSTRAP_ADMIN_EMAIL='admin@example.com'
export BOOTSTRAP_ADMIN_PASSWORD='至少12位的临时密码'

java -jar admin.jar
```

bootstrap 模式会：

- 自动执行管理端 Flyway 迁移。
- 仅在 `admin_user` 为空时创建管理员。
- 使用 BCrypt 保存临时密码。
- 自动绑定 `admin:super-admin` 角色。
- 不启动 Web、Redis、邮件和 Sa-Token。
- 完成后关闭进程。

初始化后应使用正常 profile 启动管理端，并立即修改临时密码。已有管理员时再次执行会直接失败。
