# Mini DOAMP — 联调验证 + 逐 Phase 审查报告（v2 修订版）

> **v2 修订说明**：根据 GPT-4o 交叉审查反馈，修正了以下问题：
> 1. Phase 6 "MySQL 运行态验证"曾降级为"结构实现成立"（初轮仅实测 H2），后续已完成 MySQL 运行态验证并恢复 ✅
> 2. 消息记录解释修正：消费者未启动 ≠ 消息表没数据
> 3. 安全审查拆分为 6 项，补充密码哈希/日志脱敏/Redis 竞态窗口
> 4. 修正前端文件路径引用

---

## 一、端到端联调验证

### 环境
- Docker 容器：MySQL 8.0 / Redis 7 / RabbitMQ 3（全部启动成功）
- 后端：端口 9999
  - **H2 模式**：`bootRun --spring.profiles.active=h2`（用于主要页面功能测试）
  - **MySQL 模式**：默认 profile，连接 Docker MySQL 8.0.45（用于 Phase 6 运行态验证）
- 前端：Vue CLI dev server，端口 8156

### 全页面测试结果

| 页面 | 状态 | 说明 |
|------|------|------|
| 登录页 | ✅ | admin/admin123 登录成功，JWT token 正确注入 |
| 首页/仪表盘 | ✅ | 显示 Phase 进度、核心特性、快速入口 |
| 预警规则列表 | ✅ | CRUD 正常，创建 CPU_WARN_01 成功 |
| 预警规则编辑 | ✅ | 指标选择、通知方式多选、接收人选择、Cron 配置均正常 |
| 手动触发预警 | ✅ | 触发后生成 1 条预警记录（RUNNING, L1, 值=83.9, 阈值>80, 分组=近2期均值） |
| 预警记录 | ✅ | 正确显示触发记录，过滤条件可用 |
| 消息记录 | ⚠️ | 页面正常加载但无数据。H2 模式下 MQ 消费者未自动启动（`auto-startup=false`），但**生产者仍会先落库 MsgRecord（PENDING）再投 MQ**（`WarnMessageProducer.java:48`），投递失败会回写 FAILED（`WarnMessageProducer.java:70`）。当前无消息记录说明该规则的接收人/通知方式配置可能未被正确匹配，**不等于消费者关闭就天然没数据**。需进一步排查。 |
| 流程模板 | ✅ | 创建 TEST_FLOW_01 成功，状态=草稿 |
| 流程设计器 | ✅ | AntV X6 画布加载，默认生成 开始→处理→结束，工具栏含 6 种节点类型 |
| 任务模板 | ✅ | 页面正常加载 |
| 任务执行中心 | ✅ | 页面正常加载 |
| 字典管理 | ✅ | 4 个字典（index_type/warn_level/notify_type/task_status），含"刷新缓存"按钮 |
| 定时任务监控 | ✅ | 3 个 handler（warn_check/sop_generate/msg_compensation），执行日志区域可用 |

---

## 二、逐 Phase 代码审查（对照 agents.md 简历描述）

> **注意：Phase 0（项目骨架 + 建表 SQL）和"全部完成 | 整体项目"未在本轮独立审查。**

### Phase 1：预警引擎 ✅

| 审查项 | 结果 | 说明 |
|--------|------|------|
| 策略模式（WarnStrategy 接口 + 7 实现类） | ✅ | `WarnStrategy.java` 接口 + 7 个 `@Component` 实现（Running / Operation / Employee / CustomSql / Channel / Branch / Bank） |
| 7 种指标类型查询逻辑各不相同 | ✅ | Running: 最新值 + 7期滑动均值；Bank: 逐银行 + 超限占比；Channel: 绝对值 + 环比 ±20%；CustomSql: JdbcTemplate 执行用户 SQL |
| 自定义 SQL 注入防护 | ✅ | `CustomSqlValidator.java` 6 层白名单校验：基础检查 → 语法子集 → 表白名单(6张) → 函数白名单(22个) → value 别名 → 结构禁止（禁 UNION/子查询/INTO OUTFILE） |
| 指标主表 + 阈值子表独立设计 | ✅ | `WarnIndex` + `WarnThreshold` 独立实体，与事件表解耦 |
| WarnEngine 通过指标类型路由 | ✅ | `Map<IndexType, WarnStrategy>` via `@PostConstruct`，无 if-else |
| 新增类型只需新增策略类 | ✅ | 新增 `@Component` 实现 `WarnStrategy` 即自动注册到 strategyMap |

**关键代码位置：**
- `mini-doamp-event/.../event/engine/WarnEngine.java`
- `mini-doamp-event/.../event/strategy/` （接口 + AbstractBase + 7 个策略）
- `mini-doamp-event/.../event/util/CustomSqlValidator.java`

### Phase 2：消息推送 ✅

| 审查项 | 结果 | 说明 |
|--------|------|------|
| Topic Exchange | ✅ | `RabbitMqConfig.java`: `new TopicExchange("warn.exchange")` |
| 3 个独立消费队列 | ✅ | `sms.queue` / `email.queue` / `wxwork.queue`，各绑定独立 routing key |
| 幂等消费基于消息唯一 ID + Redis SETNX | ✅ | `AbstractMsgConsumer.java`: `SETNX msg:idempotent:{msgId}` + 24h TTL |
| DLX + DLQ 配置 | ✅ | 每个队列 `.deadLetterExchange(WARN_DLX_EXCHANGE).deadLetterRoutingKey("dlq")`，`warn.dlq` 绑定 `warn.dlx.exchange` |
| 重试最多 3 次，超限标记 ALARM | ✅ | `MsgCompensationJob.java`: `MAX_RETRY=3`，`retryCount >= 3` → `MsgStatus.ALARM` |
| 消息流水表状态流转完整 | ✅ | MsgStatus 枚举：PENDING → SENT / FAILED → RETRYING → ALARM |
| 实际发送为 Mock | ✅ | `mockSend()` 只打印日志，不调第三方 |

**关键代码位置：**
- `mini-doamp-event/.../event/config/RabbitMqConfig.java`
- `mini-doamp-event/.../event/mq/producer/WarnMessageProducer.java`
- `mini-doamp-event/.../event/mq/consumer/AbstractMsgConsumer.java`
- `mini-doamp-event/.../event/job/MsgCompensationJob.java`

### Phase 3：Redis 缓存 ✅

| 审查项 | 结果 | 说明 |
|--------|------|------|
| Cache Aside 模式 | ✅ | `CacheService.java`: `get()` → miss → DB → `set()` 回填 |
| 延迟双删 | ✅ | `doubleDelete()`: 删缓存 → `schedule(500ms)` → 再删 |
| 随机 TTL 防雪崩 | ✅ | `randomTtl()` = baseTTL(30min) ± offset(5min)，使用 `ThreadLocalRandom` |
| 空值缓存防穿透 | ✅ | `setNull()`: 写入 `__NULL__` 标记 + 5min 短 TTL |
| 手动刷新缓存 REST 接口 | ✅ | `CacheController` + 前端"刷新缓存"/"刷新全部缓存"按钮 |
| SCAN 替代 KEYS | ✅ | `scanKeys()` 用 `Cursor<String>` + `try-with-resources`，避免阻塞 Redis |

### Phase 4：定时调度 ✅

| 审查项 | 结果 | 说明 |
|--------|------|------|
| XXL-Job 集成 | ✅ | `@XxlJob("warnCheckHandler")` / `@XxlJob("sopTaskGenerateHandler")` / `@XxlJob("msgCompensationHandler")` |
| ShedLock 动态锁名（预警） | ✅ | `WarnCheckJob.java`: `"warn_check_" + rule.getId()` |
| ShedLock 动态锁名（SOP） | ✅ | `SopTaskGenerateJob.java`: `"sop_generate_" + tpl.getId()` |
| 不同模板同周期可并行 | ✅ | 每个 rule/template 独立锁名，不互相抢锁 |
| 编程式 LockProvider.lock() | ✅ | 不用 `@SchedulerLock` 注解，用 `lockProvider.lock(lockConfig)` 编程式调用 |

### Phase 5：SOP 工作流 ✅

| 审查项 | 结果 | 说明 |
|--------|------|------|
| 状态机（枚举 + 转换矩阵） | ✅ | `TaskStatus.canTransitTo()` 枚举方法校验合法转换 |
| 状态流转完整 | ✅ | CREATED → PENDING_ASSIGN → EXECUTING → APPROVING → COMPLETED/REJECTED/TERMINATED |
| `WorkflowEngine.advance()` | ✅ | 296 行，含状态校验、策略执行、节点推进、日志写入、MQ 通知 |
| 策略模式处理节点 | ✅ | `Map<String, NodeHandler>` 路由，Process/Approve/Copy/Branch 各有不同逻辑 |
| 回退到任意已完成节点 | ✅ | `rollback()`: 校验目标节点有 DONE 记录才允许回退 |
| 回退标记 ROLLED_BACK | ✅ | 中间 exec 标记 `TaskExecStatus.ROLLED_BACK`，不物理删除 |
| 操作流水表 | ✅ | `taskService.writeLog()` 记录操作人、时间、操作类型、前后状态、备注 |
| 状态变更触发 MQ 通知 | ✅ | `sendNotification()` → `sopNotifier.send()` 复用 Phase 2 的 RabbitMQ |
| 前端 AntV X6 流程设计器 | ✅ | 已实测画布加载、6 种节点类型工具栏（开始/处理/审批/抄送/分支/结束） |
| 流程查看器高亮当前节点 | ✅ | FlowViewer 组件为 X6 只读模式 |

**关键代码位置：**
- `mini-doamp-sop/.../sop/engine/WorkflowEngine.java`
- `mini-doamp-sop/.../sop/engine/NodeHandler.java` + 实现
- `mini-doamp-vue/src/views/sop/WorkflowDesigner.vue`（页面）→ 内部引用 `components/FlowDesign/FlowDesigner.vue`（X6 组件）

### Phase 6：多库适配 ✅

| 审查项 | 结果 | 说明 |
|--------|------|------|
| databaseIdProvider 自动识别 | ✅ | `MybatisPlusConfig.java:36` 中 `VendorDatabaseIdProvider` 动态检测数据库类型 |
| DatabaseAdapter 抽象接口 | ✅ | `DatabaseAdapter.java`: `dateFormat()` / `paginate()` / `groupConcat()` 三个抽象方法 |
| 适配器工厂模式 | ✅ | `DatabaseAdapterFactory.java`: `Map<String, DatabaseAdapter>` 按 `databaseId()` 索引 |
| 2-3 个方言差异示例 | ✅ | 日期格式化（DATE_FORMAT vs FORMATDATETIME）、分页语法（LIMIT vs LIMIT/OFFSET）、字符串聚合（GROUP_CONCAT vs LISTAGG） |
| MySQL + H2 双模式可运行 | ✅ | H2 模式已完成全页面功能测试；默认 MySQL profile 已补验证：`/api/db/info` → `databaseId=mysql, adapterClass=MysqlAdapter, productName=MySQL, productVersion=8.0.45`；`/api/db/dialect-demo` → 返回 `DATE_FORMAT` / `GROUP_CONCAT` / `LIMIT`（MySQL 风格）；3 个真实业务接口（`/warn/records/trend` / `/warn/indexes/type-summary` / `/system/job/log/native`）均 200；登录接口同实例 200。 |

### Phase 7：前端 ✅

| 审查项 | 结果 | 说明 |
|--------|------|------|
| 组件 PascalCase 命名 | ✅ | FlowDesigner / FlowViewer / WarnRuleEdit 等 |
| API 按模块组织 | ✅ | `api/warn.js` / `api/sop.js` / `api/system.js` / `api/auth.js` |
| Vuex 管理全局状态 | ✅ | `store/modules/user.js`: token / profile / permissions |
| Axios 统一封装拦截器 | ✅ | `utils/request.js`: JWT Bearer 注入、401 自动 refresh rotation、403 跳转 dashboard |
| AntV X6 流程设计器 | ✅ | 已实测画布加载和节点工具栏 |
| X6 只读模式高亮当前节点 | ✅ | FlowViewer 组件 |

### 安全审查

#### 1. 密码哈希与日志脱敏 ✅
- **密码哈希**：`SecurityConfig.java:57` 注册 `BCryptPasswordEncoder`；`UserService.java:67` 用 `passwordEncoder.encode()` 存储
- **日志脱敏**：全项目 `log.*(password|密码)` 搜索结果为空 — 无任何日志语句直接打印密码字段

#### 2. JWT 与黑名单 ✅
- access token(24h) + refresh token(7d)
- `JwtAuthFilter.java:38` 校验 `type=access`，业务接口只认 access token
- `AuthService.java:54` refresh 端点：旧 refresh token 立刻拉黑（`TokenBlacklistService.java:23`）
- refresh rotation：每次刷新换发新 access + 新 refresh

#### 3. 权限校验 ✅
- Spring Security Filter Chain + 路由级 `permission` 元数据
- Vuex `hasPermission` + `router.beforeEach` 前端路由守卫

#### 4. Redis 竞态窗口与缓解 ⚠️
- **存在竞态窗口**：延迟双删是工程折中，不是无竞态（`CacheService.java:79`）
- **缓解措施**：事务提交后触发 `doubleDelete()`（`DictService.java:184` 在 `@Transactional` 之后调用）+ 空值缓存 + 随机 TTL
- **幂等操作**：SETNX 原子操作用于 MQ 消费幂等；ShedLock 保证分布式调度互斥

#### 5. SQL 注入防护 ✅
- 6 层白名单 + 25 个单元测试 (`CustomSqlValidatorTest.java`)

#### 6. 前端防 XSS ✅
- 未发现 `v-html` 直接渲染用户输入

---

## 三、遗漏项声明

本轮审查**未覆盖**以下 agents.md 要求的审查项：
1. **Phase 0**（项目骨架 + 建表 SQL）— 未独立审查表结构合理性
2. **"全部完成 | 整体项目"**（简历 8 条逐条核对、端到端可运行）— 未作为独立项审查

---

## 四、面试高危追问点（GPT-4o 建议）

1. **多库切换怎么证明真的跑过？** → 分开讲"结构证据"（Adapter+Factory+databaseIdProvider）和"运行证据"（H2 / MySQL 均已跑通），面试时仍要区分这两类证据
2. **H2 下消息页面为什么没数据？** → 消费者关闭只影响消费端；是否有 MsgRecord 取决于规则有无通知方式+接收人，以及生产者是否已落库
3. **JWT refresh rotation 链路？** → `AuthService.java:54` → 旧 refresh 立刻拉黑 → `JwtAuthFilter.java:38` 只认 access → 业务接口不接受 refresh token
4. **Redis 一致性有没有竞态？** → 有窗口，但用 afterCommit + delayed double delete + 空值缓存 + 随机 TTL 做了可接受的工程折中
5. **SOP 任务生命周期全靠 advance() 吗？** → 创建阶段在 `SopTaskService.java:97` 完成 CREATED → PENDING_ASSIGN → EXECUTING；运行期推进/驳回/回退才进入 `WorkflowEngine.advance()`

---

## 五、总结

> **结论：简历第 1/2/3/4/5/7 条全部代码实现到位，结构 + 运行态均验证通过。**
> 第 6 条（慢查询优化）和第 8 条（gRPC 通信）为口述项，不审查代码。

| 维度 | 评价 |
|------|------|
| **代码质量** | 架构清晰，设计模式运用恰当（策略/状态机/适配器工厂），模块解耦良好 |
| **简历对齐** | 6 条代码实现项均与简历描述精确对应，Phase 6 MySQL 运行态已补验证 |
| **安全性** | BCrypt 密码哈希 + 日志无密码泄露 + 6 层 SQL 校验 + JWT 双 token + Redis 竞态有缓解措施 |
| **可演示性** | H2 + MySQL 双模式均可跑，手动触发→预警记录生成。MQ 消费链路需 MySQL 模式演示 |

---

## 六、L4 全栈运行态补充验证（2026-03-25 Antigravity）

> 以下为 L4 阶段补充的运行态证据，与 `docs/gpt_review_prompt.md` V3 版对齐。

| 验证项 | 结果 | 证据 |
|--------|------|------|
| Token Rotation | ✅ | 旧 refresh → 401 (Redis 黑名单) |
| RabbitMQ 预警链路 | ✅ | 触发 → 20 条 SENT 消息 + 10 条预警记录 |
| SOP 通知链路 | ✅ | SUBMIT → +1 msg(SENT,"SOP任务状态变更"), APPROVE → +1 msg |
| Redis 缓存操作 | ✅ | dict/index/all refresh 全部 200 |
| ShedLock 并行 | ✅ | 3 rules concurrent → [200,200,200] |
| MySQL vs H2 | ✅ | DB Adapter=mysql, 8 个 API 同构 |
| SOP 生命周期 | ✅ | EXECUTING → APPROVING → COMPLETED, 5 条操作流水 |

