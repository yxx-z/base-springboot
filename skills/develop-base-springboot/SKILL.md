---
name: develop-base-springboot
description: base-springboot 仓库的项目级开发 Router，按任务类型和影响面加载事实来源、领域规范与验证要求。
---

# 开发 Base SpringBoot

## 事实门

1. 完整读取仓库根目录 `AGENTS.md`，执行 `git status --short`，保留用户已有改动。
2. 使用 `rg` 检查相关代码、POM、配置、迁移和测试；当前实现及其测试是“现状事实”。
3. Reference 记录稳定的“目标规范”和少量现状索引。发现两者不一致时，先判定为已批准的目标变更或实现漂移：
   - 已批准的稳定变更：实现与对应 Reference 同步更新。
   - 未获批准或与约束冲突：按实现漂移报告或修复，不得修改 Reference 为缺陷背书。

完成标准：结论同时说明现状证据和适用的目标规范，不根据 Skill 猜测当前实现。

## 任务分支

先选择一个分支，再执行 Reference 路由：

- **只读诊断**：用于解释、审计、评审和定位原因。只做非变更检查，以文件、符号、配置或测试结果支撑结论；用户未要求修复时不落盘。
- **仅验证**：用于测试、构建和复现。运行与风险匹配的检查，并核对测试报告中的执行数、失败、错误和跳过，不能只看退出码。
- **落盘修改**：用于开发、重构、配置或文档更新。建立影响面和验证策略，完成最小完整修改并同步所有直接消费者。

完成标准：任务始终属于一个明确分支；若用户追加了修改要求，切换到“落盘修改”并补齐其前置条件。

## Reference 路由

按影响面读取**所有命中项**，不得选择其中一份代替其余项：

- 模块职责、POM、依赖方向、代码放置：[architecture.md](references/architecture.md)
- Java 设计、注释、命名、异常、错误码、事务和并发：[coding-conventions.md](references/coding-conventions.md)
- 登录、Token、Session、密码、RBAC 和安全边界：[security-auth.md](references/security-auth.md)
- 表结构、索引、Flyway 和初始化数据：[database-migrations.md](references/database-migrations.md)
- Controller、统一响应、TraceId、审计和 Logback：[api-audit-logging.md](references/api-audit-logging.md)
- application YAML、Profile、环境变量、Feature 和 bootstrap：[configuration.md](references/configuration.md)
- Forest、出站 HTTP、第三方 Client、重试和 MockWebServer：[http-client.md](references/http-client.md)
- 测试选择、Testcontainers、构建和交付：[testing.md](references/testing.md)

Java 修改必须命中 `coding-conventions.md`；任何落盘修改及仅验证任务必须在实施或执行前命中 `testing.md`。

完成标准：每个命中的 Reference 均已完整读取；其适用检查有证据，确实不适用的检查明确标记原因。

## 执行与完成门

1. 列出入口、核心实现、数据与配置、直接消费者、测试和兼容性影响。
2. 按 Reference 确认职责边界，以最小完整变更覆盖全部影响面。
3. 按 `testing.md` 从定向检查逐步扩展到风险所需层级；公共模块先证明消费者清单，存在受影响消费者时验证，没有时验证模块契约并明确报告。
4. 检查旧引用、补丁格式、测试是否真实执行以及工作区是否出现无关文件。
5. 交付时报告结果、实际验证、兼容性影响和仍存在的风险；无法执行或被跳过的验证明确记为未完成。

## Skill 同步门

“落盘修改”完成实现和所需验证后、交付前必须执行；只读诊断和仅验证任务不修改 Skill。

1. 对照事实门记录的初始工作区状态、最终 `git status --short` 和本次实际差异，复核模块职责、依赖方向、跨任务复用的公共契约、配置默认值、迁移策略、领域规则和验证入口。
2. 按 Reference 路由逐项判断：
   - 不更新会使后续同类任务沿用错误的职责、契约、默认值或验证流程：在同一任务中更新所有相关 Reference；出现现有路由无法准确承载的新领域时，新增 Reference 并补充路由。
   - 仅实现既有规范内的业务功能、修复未改变稳定契约的缺陷，或变化属于临时故障、固定测试数量、机器路径、凭据和易失效行号：保持 Skill 不变。
   - 发现未获批准的实现漂移：报告或修复，不通过修改 Skill 为其背书。
3. Skill 有修改时按 `testing.md` 完成验证，并针对同一业务变更回查一次同步判断；没有新增稳定变化后结束，不递归扩写。
4. 交付时明确写出 `Skill 同步判断：已更新 <文件>` 或 `Skill 同步判断：无需更新，<原因>`，不依赖 PR 模板或人工提醒。

完成标准：每次落盘修改都有可核对的同步结论；需要更新的 Reference、路由和验证入口已在当前任务中闭合。

只有在所有命中规范均已处理、影响面已闭合、所需验证真实通过、工作区范围正确且 Skill 同步门已闭合时，才能报告完成。
