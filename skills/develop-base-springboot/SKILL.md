---
name: develop-base-springboot
description: Develop, diagnose, review, refactor, test, and document the base-springboot project. Use when working on this repository's Java or Spring Boot code, Maven modules and dependencies, admin or business applications, common modules, authentication and RBAC, Flyway migrations, application YAML, Logback, audit logging, Testcontainers, architecture tests, or production-readiness checks.
---

# 开发 Base SpringBoot 基础框架

## 总体原则

把当前仓库代码、POM、迁移文件和测试视为最终事实来源。先检查，再判断，不根据本文档猜测已经变化的实现。

保持基础框架通用、可裁剪、职责清晰。不要为了眼前复用把业务规则塞进公共模块，也不要为了“未来可能使用”增加长期为空的字段、配置或依赖。

## 开始任务

1. 完整读取仓库根目录 `AGENTS.md`。
2. 执行 `git status --short`，识别并保留用户已有改动。
3. 使用 `rg` 定位实现、调用方、配置、迁移和测试；不要只修改第一个命中的文件。
4. 判断请求类型：
   - 审计、解释、诊断：只读检查并给出证据，除非用户同时要求修改。
   - 修改、构建、优化：完成实现、测试和必要文档更新。
5. 根据任务读取下列 reference；不要一次加载无关文档。

## Reference 路由

- 模块职责、POM、依赖方向、代码放置：读取 [references/architecture.md](references/architecture.md)。
- Java 代码、注释、命名、异常和事务：读取 [references/coding-conventions.md](references/coding-conventions.md)。
- 登录、Token、Session、密码、RBAC、安全：读取 [references/security-auth.md](references/security-auth.md)。
- 表结构、索引、Flyway、初始化数据：读取 [references/database-migrations.md](references/database-migrations.md)。
- Controller、统一响应、TraceId、审计、Logback：读取 [references/api-audit-logging.md](references/api-audit-logging.md)。
- application YAML、Profile、环境变量、Feature 开关、bootstrap：读取 [references/configuration.md](references/configuration.md)。
- 测试选择、Testcontainers、构建和交付：读取 [references/testing.md](references/testing.md)。

修改 Java 代码时同时读取 coding-conventions；任何落盘修改完成后都读取 testing。

## 标准实施流程

1. 建立影响面：列出入口、核心实现、数据模型、配置、迁移、测试和文档。
2. 确认职责边界：决定代码应留在 admin/business，还是进入某个具体 common 模块。
3. 采用最小完整修改：同步更新所有直接消费者，不做与目标无关的大规模整理。
4. 保护兼容性：明确 API、数据库、配置键、Token、缓存 Key 和部署行为是否变化。
5. 逐级验证：先编译或定向测试，再按风险运行集成测试或全量 `clean verify`。
6. 做静态收尾：检查旧名称、弃用 API、删除资源的 target 残留、补丁格式和工作区范围。
7. 交付时说明结果、验证命令、兼容性影响和仍存在的真实风险。

## 强制约束

- 使用 Java 17 和仓库自带的 Maven Wrapper。
- 优先通过构造器注入；不要新增字段注入。
- 不在公共模块引用 admin/business 的类。
- 不让 admin 与 business 互相依赖。
- 不在 Controller 中堆积事务性业务逻辑。
- 不记录密码、验证码、Token、授权头、私钥、签名或完整敏感请求。
- 不把功能开关关闭解释为 Maven 依赖已移除。
- 不修改已经进入正式环境的历史 Flyway 迁移；初始框架阶段修改基线前也要明确数据库必须重建。
- 不用测试退出码 0 证明 Testcontainers 已执行；检查测试是否因 Docker 不可用而跳过。
- 不删除用户生成的日志、数据库、缓存或工作区改动，除非用户明确授权。

## 使用验证脚本

从仓库根目录运行：

```bash
skills/develop-base-springboot/scripts/verify-project.sh static
skills/develop-base-springboot/scripts/verify-project.sh compile
skills/develop-base-springboot/scripts/verify-project.sh module common/common-security,admin
skills/develop-base-springboot/scripts/verify-project.sh architecture
skills/develop-base-springboot/scripts/verify-project.sh full
```

脚本只是稳定入口。遇到失败时读取原始 Maven/Testcontainers 输出，不通过屏蔽、跳过或降低断言规避失败。

## 更新 Skill

架构、认证契约、迁移策略或验证命令发生稳定变化时，同步更新对应 reference。不要把临时故障、固定测试数量、机器路径、凭据或很快失效的行号写入 Skill。
