# Mini DOAMP 开发分阶段计划

> 每个 Phase 完成后暂停等待确认，可选提交 Codex 审查（参考 agents.md）

## Phase 0：项目骨架

### 目标
搭建 Gradle 多模块项目结构、基础配置、建表 SQL、通用工具类

### 任务清单

1. 初始化 Gradle 多模块项目（7个模块）
2. 配置 `build.gradle`（根 + 各子模块依赖）
3. 配置 `settings.gradle` 注册所有模块
4. 编写 `application.yml`（MySQL、Redis、RabbitMQ、XXL-Job 连接配置）
5. 编写 `log4j2.xml` 日志配置
6. 编写全量建表 SQL（`schema.sql`）
7. 编写初始化数据 SQL（`data.sql`，含管理员账号、基础字典、示例指标数据）
8. 实现通用响应类 `R<T>`（ok/fail/data）
9. 实现全局异常处理 `GlobalExceptionHandler`
10. 实现 `BusinessException` 业务异常类
11. 实现基础枚举（IndexType、WarnLevel、TaskStatus、NodeType 等）
12. 配置 MyBatis Plus（分页插件、自动填充、逻辑删除——业务主表含 deleted 字段）
13. 配置 MapStruct 依赖
14. Application 启动类
15. 实现 Spring Security + JWT 认证（登录、Token 签发与校验、Filter 链）
16. 实现用户 CRUD 接口（Controller + Service + Mapper）
17. 实现角色 CRUD 接口
18. 实现部门树查询 + CUD 接口

### 验收标准
- [ ] 项目能正常编译（`gradle build`）
- [ ] Application 能启动并连接 MySQL
- [ ] 建表 SQL 执行无报错，20 张业务表 + 1 张 shedlock 框架表全部创建
- [ ] 初始化数据插入成功
- [ ] 登录接口返回 JWT Token，携带 Token 可访问受保护接口
- [ ] 用户/角色/部门 CRUD 接口可用

## Phase 1：预警引擎（简历第1条）

### 目标
实现策略模式预警引擎，支持 7 种指标类型动态路由

### 任务清单

1. 编写预警相关实体类（WarnIndex、WarnThreshold、WarnRule、WarnRecord）
2. 编写对应 Mapper 接口 + XML
3. 定义 `WarnStrategy` 接口：`List<WarnRecord> check(WarnIndex index, List<WarnThreshold> thresholds)`
4. 实现 7 个策略类：
   - `RunningWarnStrategy` — 查 t_index_running，直接对比阈值
   - `OperationWarnStrategy` — 查 t_index_operation，直接对比阈值
   - `BankWarnStrategy` — 查 t_index_group(BANK)，按银行分组逐个对比
   - `ChannelWarnStrategy` — 查 t_index_group(CHANNEL)，按渠道分组
   - `EmployeeWarnStrategy` — 查 t_index_group(EMPLOYEE)，按员工分组
   - `BranchWarnStrategy` — 查 t_index_group(BRANCH)，按营业部分组
   - `CustomSqlWarnStrategy` — 执行用户配置 SQL（含安全校验），对比阈值
5. 实现 `WarnEngine`：接收规则ID → 查指标 → 路由策略 → 执行检查 → 写入预警记录
6. 实现自定义 SQL 安全校验（白名单关键字 + 禁止 DROP/DELETE/UPDATE/INSERT）
7. 编写预警指标 CRUD 接口（Controller + Service）
8. 编写预警规则 CRUD 接口
9. 编写预警记录查询接口
10. 编写手动触发预警检查接口

### 验收标准
- [ ] 7 个策略类全部实现，查询逻辑各不相同
- [ ] WarnEngine 通过 `Map<IndexType, WarnStrategy>` 路由，无 if-else
- [ ] 新增指标类型只需新增策略类 + 注册，不改引擎代码
- [ ] 自定义 SQL 类型有注入防护
- [ ] 手动触发接口能正确生成预警记录

## Phase 2：消息推送（简历第2条）

### 目标
实现 RabbitMQ 异步消息推送，含幂等消费、死信队列、补偿重试

### 任务清单

1. 编写消息记录实体类（MsgRecord）+ Mapper
2. 配置 RabbitMQ：
   - Topic Exchange：`warn.exchange`
   - 3 个队列：`sms.queue`、`email.queue`、`wxwork.queue`
   - Routing Key：`warn.sms`、`warn.email`、`warn.wxwork`
   - 死信交换机：`warn.dlx.exchange` + 死信队列：`warn.dlq`
3. 实现 `WarnMessageProducer`：预警记录生成后按通知方式发送到对应队列
4. 实现 3 个消费者（Mock 实现，只打印日志）：
   - `SmsConsumer`
   - `EmailConsumer`
   - `WxWorkConsumer`
5. 实现幂等消费：消费前用 msg_id 查 Redis SET，已存在则跳过
6. 消费失败进入死信队列
7. 实现补偿重试定时任务：扫描 FAILED 状态消息，重试最多 3 次，超限标记 ALARM
8. 编写消息记录查询接口
9. 编写手动重试接口
10. 编写消息统计接口

### 验收标准
- [ ] 使用 Topic Exchange（非 Direct/Fanout）
- [ ] 3 个独立消费队列各有对应消费者
- [ ] 幂等消费基于 msg_id + Redis SET
- [ ] 死信队列正确配置（DLX + DLQ）
- [ ] 重试最多 3 次，超限标记 ALARM
- [ ] 消息流水表记录完整状态流转
- [ ] 实际发送为 Mock（只打日志）

## Phase 3：Redis 缓存（简历第3条）

### 目标
实现 Cache Aside 模式 + 延迟双删 + 防雪崩 + 防穿透四种缓存策略

### 任务清单

1. 配置 RedisTemplate（序列化方式：Jackson2JsonRedisSerializer）
2. 实现 `CacheService` 通用缓存服务：
   - `getWithPenetrationProtect(key, loader)` — 查缓存 → miss 查 DB → 空值缓存 5min
   - `putWithRandomTtl(key, value, baseTtl)` — 基础 TTL + 随机偏移（30min ± 5min）
   - `delayedDoubleDelete(key, dbUpdater)` — 删缓存 → 执行更新 → 延迟 500ms → 再删
3. 实现字典缓存服务 `DictCacheService`：
   - 按 dictCode 缓存字典项列表
   - key 格式：`dict:items:{dictCode}`
4. 实现指标缓存服务 `IndexCacheService`：
   - 缓存高频指标查询结果
   - key 格式：`index:data:{indexCode}:{dataDate}`
5. 字典 CRUD 接口中集成延迟双删
6. 编写手动刷新缓存 REST 接口（字典/指标/全部）

### 验收标准
- [ ] Cache Aside 模式正确（读：缓存→DB→回填；写：更新DB→删缓存）
- [ ] 延迟双删实现（更新DB→删缓存→延迟500ms→再删缓存）
- [ ] 热点数据 TTL 有随机偏移（防雪崩）
- [ ] 空值缓存并设短 TTL（防穿透）
- [ ] 提供手动刷新缓存 REST 接口
- [ ] 缓存 key 有前缀区分

## Phase 4：定时调度（简历第4条）

### 目标
集成 XXL-Job + ShedLock，实现动态锁名称的分布式定时调度

### 任务清单

1. 集成 XXL-Job（添加依赖、配置 admin 地址、注册 executor）
2. 集成 ShedLock（添加依赖、配置 LockProvider 使用 MySQL shedlock 表）
3. 实现预警检查定时任务 `WarnCheckJob`：
   - 查询所有启用的预警规则
   - 按规则逐个调用 WarnEngine 执行检查
   - ShedLock 锁名动态生成：`warn_check_${ruleId}`
4. 实现 SOP 任务定时生成 `SopTaskGenerateJob`：
   - 查询所有 CRON 触发的任务模板
   - 按模板逐个创建任务实例
   - ShedLock 锁名动态生成：`sop_generate_${templateId}`
5. 编写定时任务监控接口（任务列表、执行日志）

### 验收标准
- [ ] 使用 XXL-Job（非 @Scheduled）
- [ ] ShedLock 锁名称动态生成，格式为 `任务类型_${ID}`
- [ ] 不同规则/模板在同一调度周期能并行执行（不互相抢锁）
- [ ] 定时任务包含：预警检查 + SOP 任务生成

## Phase 5：SOP 工作流（简历第7条）

### 目标
实现状态机驱动的工作流引擎，支持流程设计、任务管理、回退任意节点

### 任务清单

**第一层：流程定义（设计态）**

1. 编写流程相关实体类（Workflow、Node、Edge）+ Mapper
2. 实现流程定义 CRUD 接口（含节点和连线的级联保存）
3. 实现流程发布/停用接口

**第二层：任务管理（运行态）**

4. 编写任务相关实体类（TaskTemplate、Task、TaskExec、OperationLog）+ Mapper
5. 实现任务模板 CRUD 接口
6. 实现手动创建任务接口（根据流程定义自动分配第一个节点执行人）
7. 实现任务列表/详情查询接口（含流程图数据 + 当前节点标识）

**第三层：流程推进引擎（核心）**

8. 定义 `TaskStatus` 枚举 + 状态转换矩阵
9. 实现节点处理策略（策略模式）：
   - `ProcessNodeHandler` — 记录执行结果
   - `ApproveNodeHandler` — 记录审批意见
   - `CopyNodeHandler` — 发送通知（复用 RabbitMQ）
   - `BranchNodeHandler` — 评估条件选择分支
10. 实现 `WorkflowEngine.advance(taskExecId, action, params)`：
    - 查询当前节点和状态
    - 校验状态转换合法性
    - 执行当前节点逻辑
    - 判断下一节点并推进
    - 写入操作流水表
    - 触发消息通知
11. 实现回退机制：
    - 支持回退到任意已完成节点
    - 记录节点执行栈，回退时按栈回溯
    - 将中间节点执行记录标记为 ROLLED_BACK（不物理删除）
12. 实现待办/已办查询接口

### 验收标准
- [ ] 使用状态机模式（枚举状态 + 转换矩阵），非硬编码
- [ ] 状态流转完整：创建→待分配→执行中→审批中→已完成/已驳回/已终止（回退后回到执行中，TaskExec 标记 ROLLED_BACK）
- [ ] `WorkflowEngine.advance()` 存在且逻辑正确
- [ ] 不同节点类型用策略模式处理
- [ ] 支持回退到任意已完成节点，中间执行记录标记为 ROLLED_BACK
- [ ] 每次状态变更写入操作流水表
- [ ] 状态变更后触发消息通知（复用 RabbitMQ）

## Phase 6：多库适配（简历第5条）

### 目标
实现数据库 Adapter 抽象层，基于 databaseIdProvider 自动识别数据库类型

### 任务清单

1. 配置 MyBatis `databaseIdProvider`（自动识别 MySQL/H2）
2. 定义 `DatabaseAdapter` 抽象接口：
   - `dateFormat(column, pattern)` — 日期格式化
   - `paginate(sql, offset, size)` — 分页语法
   - `groupConcat(column)` — 字符串聚合
3. 实现 `MysqlAdapter`
4. 实现 `H2Adapter`
5. 实现 `DatabaseAdapterFactory`（根据 databaseId 返回对应 Adapter）
6. 在 Mapper XML 中使用 `databaseId` 属性编写差异化 SQL 示例
7. 配置 H2 内嵌数据库 profile（`application-h2.yml`）
8. 验证 MySQL 和 H2 两种模式均可正常启动

### 验收标准
- [ ] 使用 MyBatis databaseIdProvider 自动识别数据库类型
- [ ] DatabaseAdapter 抽象接口已定义
- [ ] 使用适配器工厂模式获取对应实现
- [ ] 至少 2-3 个方言差异示例（日期函数、分页语法、字符串聚合）
- [ ] MySQL 和 H2 两种数据库均能正常切换运行

## Phase 7：前端实现

### 目标
实现 Vue 3 全套前端页面，含流程设计器（AntV X6）和预警管理

### 任务清单

**基础框架**

1. 初始化 Vue 3 项目（Vue CLI）
2. 集成 Ant Design Vue 3.x
3. 集成 Vuex 4.x（用户状态模块：token、权限）
4. 集成 Vue Router 4.x（路由配置 + 路由守卫）
5. 封装 Axios（请求拦截注入 JWT、响应拦截统一错误处理、403 跳转登录）
6. 实现登录页 + JWT 认证流程

**预警模块页面**

7. 预警规则列表页（CRUD、启用/禁用）
8. 预警规则编辑页（配置指标类型、阈值、通知方式）
9. 预警记录页（预警触发历史查询）
10. 消息发送记录页（消息状态、手动重试）

**SOP 工作流页面**

11. 流程设计器页（AntV X6 拖拽设计：添加节点、连线、设置属性）
12. 流程模板列表页（模板 CRUD）
13. 任务模板管理页（任务模板配置）
14. 任务执行中心页（待办/已办列表）
15. 任务详情页（任务信息 + X6 只读模式流程图 + 高亮当前节点）

**系统管理页面**

16. 字典管理页（字典 CRUD + 手动刷新缓存按钮）
17. 定时任务监控页（任务列表、执行日志）

### 验收标准
- [ ] 组件使用 PascalCase 命名
- [ ] API 文件按模块组织（api/warn.js、api/sop.js 等）
- [ ] Vuex 管理全局状态（用户信息、权限）
- [ ] Axios 统一封装请求/响应拦截器（JWT 注入、403 处理）
- [ ] 流程设计器基于 AntV X6
- [ ] 流程查看器为 X6 只读模式 + 高亮当前节点
- [ ] 前后端联调通过，核心流程可演示

## 总览

| Phase | 模块 | 简历条目 | 核心技术点 |
|-------|------|---------|-----------|
| 0 | 项目骨架 | - | Gradle多模块、建表SQL、通用工具 |
| 1 | 预警引擎 | 第1条 | 策略模式、7种指标类型、SQL注入防护 |
| 2 | 消息推送 | 第2条 | RabbitMQ Topic Exchange、幂等、死信队列、重试3次 |
| 3 | Redis缓存 | 第3条 | Cache Aside、延迟双删、随机TTL、空值防穿透 |
| 4 | 定时调度 | 第4条 | XXL-Job、ShedLock动态锁名 |
| 5 | SOP工作流 | 第7条 | 状态机、流程设计器、回退任意节点、操作流水 |
| 6 | 多库适配 | 第5条 | databaseIdProvider、Adapter工厂、MySQL/H2 |
| 7 | 前端 | - | Vue3、AntV X6、Vuex、Axios封装 |

> 第6条（慢查询优化）和第8条（gRPC通信）为口述项，不写代码。
