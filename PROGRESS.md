# Mini DOAMP 开发进度

> 每次新对话（无论 Claude 还是 Codex）请先读此文件了解当前状态。

## 前期准备

| 文件 | 状态 |
|------|------|
| CLAUDE.md | ✅ 完成，3轮审查通过 |
| agents.md | ✅ 完成，3轮审查通过 |
| docs/architecture-design.md | ✅ 完成 |
| docs/database-design.md | ✅ 完成 |
| docs/api-design.md | ✅ 完成（60个接口） |
| docs/development-phases.md | ✅ 完成（Phase 0-7） |
| docs/tech-selection.md | ✅ 完成 |
| docs/interview-qa.md | ✅ 完成 |

## 开发进度

| Phase | 内容 | 状态 | 备注 |
|-------|------|------|------|
| 0 | 项目骨架（Gradle多模块、建表SQL、通用工具、Security+JWT、用户/角色/部门CRUD） | ✅ 完成 | compileJava通过，Codex 3轮审查通过 |
| 1 | 预警引擎（策略模式、7种指标类型） | 未开始 | |
| 2 | 消息推送（RabbitMQ、幂等、死信队列、重试） | 未开始 | |
| 3 | Redis缓存（Cache Aside、延迟双删、防雪崩、防穿透） | 未开始 | |
| 4 | 定时调度（XXL-Job、ShedLock动态锁名） | 未开始 | |
| 5 | SOP工作流（状态机、流程引擎、回退任意节点） | 未开始 | |
| 6 | 多库适配（Adapter工厂、databaseIdProvider） | 未开始 | |
| 7 | 前端（Vue3、AntV X6、Vuex、Axios） | 未开始 | |

## 关键设计决策（跨文件已统一）

- 延迟双删：更新DB → 删缓存 → 延迟500ms → 再删缓存
- 回退：不产生新终态，任务回到 EXECUTING，中间 TaskExec 标记 ROLLED_BACK（不物理删除）
- ShedLock 锁名双粒度：warn_check_${ruleId} / sop_generate_${templateId}
- 流程保存：API 用 nodeCode，后端插入后映射为 nodeId
- 消息状态枚举：PENDING/SENT/FAILED/RETRYING/ALARM
- TaskStatus 终态：COMPLETED / REJECTED / TERMINATED（无"已回退"）
- JWT 双 token：access（24h）+ refresh（7d），Filter 显式校验 type=access，refresh 端点用 DB 最新用户名签发
- refresh token 无状态，jti 预留撤销扩展点，生产方案口述（Redis 黑名单/tokenVersion）
- 用户 DTO 拆分：UserCreateRequest（username+password 必填）/ UserUpdateRequest（无 username）
- 删除部门/角色前校验关联用户和子部门，创建/更新用户校验 deptId/roleId 存在性

## 工作流程

Claude 写代码 → 用户确认 → Codex 审查 → 改完进下一 Phase
