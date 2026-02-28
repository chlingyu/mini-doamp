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
| 1 | 预警引擎（策略模式、7种指标类型） | ✅ 完成 | compileJava通过，Codex 2轮审查通过 |
| 2 | 消息推送（RabbitMQ、幂等、死信队列、重试） | ✅ 完成 | compileJava通过，Codex 3轮审查通过 |
| 3 | Redis缓存（Cache Aside、延迟双删、防雪崩、防穿透） | ✅ 完成 | compileJava通过，Codex 1轮审查通过 |
| 4 | 定时调度（XXL-Job、ShedLock动态锁名） | ✅ 完成 | compileJava通过，Codex 2轮审查通过，冒烟测试通过 |
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
- 预警引擎：Map<IndexType, WarnStrategy> 路由，无 if-else；7种策略独立实现类
- 自定义 SQL 三层校验：白名单(SELECT)+黑名单(DML)+注入防护(分号/注释)，保存前+执行前两处校验
- 阈值校验：indexType/compareType 强制枚举校验，按 compareType 强制 upper/lower 必填规则
- gradle-wrapper.properties 改为腾讯云镜像
- 消息推送：Topic Exchange + 3队列(sms/email/wxwork) + DLX + DLQ，手动ACK
- 幂等消费：SETNX msg:idempotent:{msgId} 独立 key + 24h 独立过期，避免全局集合膨胀
- Producer 先存 PENDING 到 DB 再发 MQ，publish 异常回写 FAILED
- 补偿任务只扫 FAILED（不扫 RETRYING），retryCount >= 3 置 ALARM
- Redis 缓存：Cache Aside + afterCommit 延迟双删 + 随机TTL(30±5min) + 空值防穿透(__NULL__+5min)
- 缓存 key 规范：dict:items:{dictCode}、index:data:{indexCode}:{date}
- SCAN 替代 KEYS 批量删除，Cursor 用 try-with-resources 关闭
- 字典 DTO 拆分：DictRequest(创建) / DictUpdateRequest(更新，items=null不修改)
- 延迟双删线程池 @Bean(destroyMethod="shutdown") 优雅关闭
- XXL-Job：3个Handler（warnCheckHandler/sopTaskGenerateHandler/msgCompensationHandler）
- ShedLock 编程式API：动态锁名不能用@SchedulerLock注解，用LockProvider.lock()手动获取
- MsgCompensationTask 从 @Scheduled 迁移为 XXL-Job handler
- 新增 t_job_exec_log 表记录任务执行日志，JobController 提供监控API
- SopTaskGenerateJob 已实现模板扫描+动态锁 sop_generate_${templateId}，任务实例创建待 Phase 5

## 工作流程

Claude 写代码 → 用户确认 → Codex 审查 → 改完进下一 Phase
