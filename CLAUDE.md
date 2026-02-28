# Mini DOAMP - 项目开发规范

> 本文件是 Claude Code CLI 的开发指令文件。所有开发工作必须严格遵循本文件的要求。

## 1. 项目背景

面向面试准备的个人练手项目，复刻"数字化运营监控管理平台（DOAMP）"的核心后端能力。**不包含任何公司代码**，完全独立重新设计的简化版。

目标：
- 覆盖简历全部 8 条技术亮点，其中 6 条代码实现，2 条口述准备
- 前后端可运行，能现场演示
- 代码质量达到面试审查标准

## 2. 技术栈

### 后端
| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 1.8 |
| 框架 | Spring Boot | 2.7.x |
| 微服务 | Spring Cloud | 2021.0.x |
| ORM | MyBatis Plus | 3.5.x |
| 对象映射 | MapStruct | 1.5.x |
| 缓存 | Redis | - |
| 消息队列 | RabbitMQ | - |
| 定时任务 | XXL-Job | - |
| 分布式锁 | ShedLock | 4.x |
| 认证 | Spring Security + JWT | - |
| Excel | EasyExcel | 4.x |
| JSON | Fastjson2 | - |
| 简化代码 | Lombok | 1.18.x |
| 数据库 | MySQL 8.0 + H2（测试） | - |

### 前端
| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue | 3.2.x |
| 状态管理 | Vuex | 4.x |
| 路由 | Vue Router | 4.x |
| UI组件 | Ant Design Vue | 3.x |
| 图表 | ECharts | 5.x |
| 流程设计器 | AntV X6 | 2.x |
| HTTP | Axios | 0.21.x |
| 构建 | Vue CLI | 4.x |
| CSS预处理 | Less | - |

## 3. 模块与简历映射

| 模块 | 简历条目 | 核心技术点 | 实现方式 |
|------|---------|-----------|---------|
| 预警引擎 | 第1条 | 策略模式 + 7种指标类型 + 指标主表/阈值子表 | 代码实现 |
| 消息推送 | 第2条 | RabbitMQ Topic Exchange + 幂等 + 死信队列 + 重试3次 | 代码实现 |
| Redis缓存 | 第3条 | Cache Aside + 延迟双删 + 随机TTL + 空值防穿透 | 代码实现 |
| 定时调度 | 第4条 | XXL-Job + ShedLock动态锁名 | 代码实现 |
| 多库适配 | 第5条 | databaseIdProvider + Adapter工厂 + MySQL/H2 | 代码实现 |
| 慢查询优化 | 第6条 | EXPLAIN + 覆盖索引/联合索引 | **口述，不写代码** |
| SOP工作流 | 第7条 | 状态机 + 流程设计器 + 回退任意节点 | 代码实现 |
| gRPC通信 | 第8条 | Proto + TLS + Zookeeper | **口述，不写代码** |

## 4. Gradle 多模块结构

```
mini-doamp/
├── mini-doamp-server/        # 主启动模块
├── mini-doamp-core/          # 核心公共模块
├── mini-doamp-gateway/       # 网关/认证模块
├── mini-doamp-api/           # API定义模块（纯DTO/VO）
├── mini-doamp-doamp/         # 主业务模块（指标查询、字典）
├── mini-doamp-event/         # 预警事件模块（预警引擎、消息推送）
├── mini-doamp-sop/           # SOP工作流模块
├── mini-doamp-vue/           # 前端项目（独立目录）
├── build.gradle              # 根构建文件
├── settings.gradle           # 模块注册
├── gradle.properties         # 构建参数
├── CLAUDE.md                 # 本文件
├── agents.md                 # Codex审查指南
└── docs/                     # 设计文档目录
```

### 模块职责

| 模块 | 职责 | 主要内容 |
|------|------|----------|
| server | 应用启动入口 | Application.java、application.yml、log4j2.xml |
| core | 核心公共 | 实体类、Mapper、工具类、通用枚举、数据库适配器 |
| gateway | 认证网关 | Spring Security + JWT、用户服务、文件服务 |
| api | 接口定义 | Request/Response DTO、VO、常量、错误码 |
| doamp | 主业务 | 指标查询、字典管理、缓存服务 |
| event | 预警事件 | 预警引擎、策略路由、RabbitMQ消息推送 |
| sop | 工作流 | 任务管理、流程定义、状态机引擎、定时调度 |

### 模块依赖关系

```
server ──→ gateway, doamp, event, sop
gateway ──→ core, api
doamp ──→ core, api
event ──→ core, api
sop ──→ core, api
core ──→ api
```

规则：
- server 依赖所有业务模块，负责启动
- 所有业务模块依赖 core 和 api
- 业务模块之间**不直接依赖**，通过 core 公共接口通信
- core 只依赖 api

## 5. 模块一：预警引擎（简历第1条）

### 业务背景
原项目痛点：预警与事件表强耦合，新增指标需改动多处代码。重构为基于"指标主表 + 阈值子表"的独立预警引擎。

### 核心类设计

- `WarnStrategy` 接口：定义 `List<WarnRecord> check(WarnIndex index, List<WarnThreshold> thresholds)` 方法
- 7 个策略实现类，每种查询逻辑不同：

| 策略类 | 指标类型 | 查询逻辑 |
|--------|---------|---------|
| RunningWarnStrategy | 运行类 | 查指标值直接对比阈值上下限 |
| OperationWarnStrategy | 运营类 | 同上，数据来源表不同 |
| BankWarnStrategy | 银行类 | 按银行机构分组查询后逐个对比 |
| ChannelWarnStrategy | 渠道效能类 | 按渠道分组查询 |
| EmployeeWarnStrategy | 员工类 | 按员工分组查询 |
| BranchWarnStrategy | 营业部类 | 按营业部分组查询 |
| CustomSqlWarnStrategy | 自定义SQL类 | 用户配置SQL（**需安全校验防注入**），执行后对比阈值 |

- `WarnEngine` 引擎类：接收规则ID → 查关联指标和阈值 → 按指标类型路由到对应策略 → 执行查询 → 对比阈值 → 生成预警记录
- 预警规则支持配置：通知方式（短信/邮件/企业微信，可多选）、接收人列表

## 6. 模块二：消息异步推送（简历第2条）

### 业务背景
预警触发后不同步发送通知，通过 RabbitMQ 异步推送，保证可靠投递。

### 技术要求

- **Topic Exchange**：按通知方式动态路由到 3 个消费队列
  - `sms.queue` — 短信
  - `email.queue` — 邮件
  - `wxwork.queue` — 企业微信
- **幂等消费**：基于消息唯一 ID（写入 Redis SET）去重，防止重复发送
- **死信队列（DLQ）**：消费失败的消息进入死信队列
- **补偿重试**：定时任务扫描消息流水表中 status=FAILED 的记录，最多重试 **3 次**，超限标记为"告警"状态需人工处理
- **消息流水表**：每条消息记录发送状态（PENDING待发送 / SENT已发送 / FAILED发送失败 / RETRYING重试中 / ALARM告警）
- **Mock 实现**：短信/邮件/企业微信的实际发送只打印日志，不调第三方

## 7. 模块三：Redis 缓存（简历第3条）

### 缓存策略（全部需要体现）

| 策略 | 实现方式 |
|------|---------|
| Cache Aside | 读时先查缓存，miss 则查 DB 并回填；写时先更新 DB，再删缓存 |
| 延迟双删 | Cache Aside 增强：更新 DB → 删缓存 → 延迟 **500ms** → 再删一次缓存（防并发读回填旧数据） |
| 防缓存雪崩 | 热点数据设置 基础 TTL + 随机偏移（如 **30min ± 5min**） |
| 防缓存穿透 | 查询结果为空时，缓存空值并设置短 TTL（**5 分钟**） |

### 应用场景
- 字典翻译数据（从字典表加载）
- 高频指标查询结果

### 额外要求
- 提供手动刷新缓存的 REST 接口

## 8. 模块四：定时调度（简历第4条）

### 技术要求

- 集成 **XXL-Job** 作为分布式任务调度中心
- 集成 **ShedLock** 做分布式锁
- 锁名称**必须动态生成**，按任务类型区分粒度：
  - 预警检查：`warn_check_${ruleId}`（按规则粒度，因为 API 按规则触发）
  - SOP 任务生成：`sop_generate_${templateId}`（按模板粒度）
  - 原因：不同规则/模板在同一调度周期触发时，若锁名相同会导致任务被误跳过
- 定时任务列表：
  - 预警检查任务（调用模块一的 WarnEngine）
  - SOP 任务定时生成（调用模块五的任务创建）

## 9. 模块五：SOP 工作流引擎（简历第7条，最复杂）

### 第一层：流程定义（设计态）

- 流程定义表、节点表、连线表
- 节点类型枚举：

| 节点类型 | 说明 |
|---------|------|
| START | 开始节点 |
| PROCESS | 处理节点 |
| APPROVE | 审批节点 |
| COPY | 抄送节点 |
| BRANCH | 分支节点 |
| END | 结束节点 |

- 前端用 AntV X6 实现可视化流程设计器：拖拽添加节点、连线、设置节点属性（执行人、审批人等），保存到后端
- 流程模板 CRUD

### 第二层：任务管理（运行态）

- **任务模板**：关联一个流程定义，配置任务内容参数（标题、描述）和反馈参数（执行完后需要填什么）
- **任务创建**：支持手动创建和定时自动生成（由模块四触发）
- **任务分配**：任务创建后根据流程定义的节点配置自动分配执行人

### 第三层：流程推进引擎（核心）

**状态机**：基于枚举定义状态 + 状态转换矩阵

```
状态流转：创建 → 待分配 → 执行中 → 审批中 → 已完成 / 已驳回 / 已终止
（回退不是任务终态，回退后任务回到"执行中"，被回退的执行记录标记为 ROLLED_BACK）
```

**核心方法** `WorkflowEngine.advance(taskExecId, action, params)`：
1. 查询当前节点和状态
2. 校验状态转换合法性（查转换矩阵）
3. 执行当前节点逻辑（不同节点类型用**策略模式**）：
   - 处理节点：记录执行结果
   - 审批节点：记录审批意见
   - 抄送节点：发通知
4. 判断下一节点并推进：
   - 普通节点：直接推进
   - 分支节点：评估条件选择分支
   - 结束节点：关闭任务
5. 每次状态变更写入**操作流水表**（操作人、时间、类型、备注）
6. 状态变更后触发消息通知（发到 RabbitMQ，复用模块二）

**回退机制**：
- 支持回退到任意已完成节点
- 记录节点执行栈，回退时按栈回溯
- 将中间节点的执行记录标记为 ROLLED_BACK（不物理删除，保留审计痕迹）

## 10. 模块六：多数据库适配（简历第5条）

### 技术要求

- 通过 MyBatis 的 `databaseIdProvider` 机制自动识别当前数据库类型
- 设计 `DatabaseAdapter` 抽象接口 + 适配器工厂模式
- 封装数据库方言差异，做 2-3 个示例即可：

| 差异点 | MySQL | H2 |
|--------|-------|-----|
| 日期格式化 | DATE_FORMAT(col, '%Y-%m-%d') | FORMATDATETIME(col, 'yyyy-MM-dd') |
| 分页语法 | LIMIT offset, size | LIMIT size OFFSET offset |
| 字符串聚合 | GROUP_CONCAT(col) | GROUP_CONCAT(col) |

- 用 **MySQL + H2**（内嵌数据库）两种来演示切换效果

## 11. 不需要代码实现的部分

| 简历条目 | 内容 | 面试准备方式 |
|---------|------|------------|
| 第6条 | 慢查询优化 | 口述 EXPLAIN 分析过程、索引优化策略 |
| 第8条 | gRPC微服务通信 | 口述 Proto 定义、TLS 双向认证、ZK 注册发现原理 |
| - | 监控大屏 | 不做，这是前端展示工作 |
| - | 驾驶舱 | 不做，这是前端展示工作 |

## 12. 前端页面清单

### 系统基础页面
| 页面 | 路由 | 说明 |
|------|------|------|
| 登录页 | /login | JWT 登录 |
| 首页/导航 | / | 系统入口 |
| 403 | /403 | 无权限页 |

### 预警模块页面
| 页面 | 路由 | 说明 |
|------|------|------|
| 预警规则列表 | /warn/rules | 规则 CRUD、启用/禁用 |
| 预警规则编辑 | /warn/rules/edit | 配置指标类型、阈值、通知方式 |
| 预警记录 | /warn/records | 预警触发历史 |
| 消息发送记录 | /warn/messages | 消息状态、重试操作 |

### SOP 工作流页面
| 页面 | 路由 | 说明 |
|------|------|------|
| 流程设计器 | /sop/design | AntV X6 拖拽设计流程 |
| 流程模板列表 | /sop/templates | 模板 CRUD |
| 任务模板管理 | /sop/task-templates | 任务模板配置 |
| 任务执行中心 | /sop/tasks | 待办/已办列表 |
| 任务详情 | /sop/tasks/:id | 任务详情 + 流程图（X6只读高亮当前节点） |

### 系统管理页面
| 页面 | 路由 | 说明 |
|------|------|------|
| 字典管理 | /system/dict | 字典 CRUD + 手动刷新缓存 |
| 定时任务监控 | /system/jobs | 任务列表、执行日志 |

## 13. 代码规范

### 后端规范

- 统一响应：`R.ok().data(...)` / `R.fail(...)`
- 异常处理：`BusinessException` + `@RestControllerAdvice` 全局处理
- 使用 Lombok：`@Data`, `@Slf4j`, `@RequiredArgsConstructor`
- 使用 MapStruct 做对象转换（Entity ↔ DTO ↔ VO）
- Service 层加 `@Transactional`
- Controller 只做参数校验和调用 Service，不写业务逻辑

### 包命名规范

```
com.demo.minidoamp
├── controller/        # 控制器
├── service/           # 业务服务
│   └── impl/         # 服务实现
├── dao/              # 数据访问
├── entity/           # 实体类
├── mapper/           # MyBatis Mapper接口
├── config/           # 配置类
├── enums/            # 枚举
├── strategy/         # 策略类
├── engine/           # 引擎类（预警引擎、工作流引擎）
├── mq/               # MQ 生产者/消费者
│   ├── producer/
│   └── consumer/
├── adapter/          # 数据库适配器
└── util/             # 工具类
```

### 前端规范

- 组件使用 PascalCase 命名
- API 文件按模块组织（`api/warn.js`、`api/sop.js`）
- 使用 Vuex 管理全局状态
- 使用 Less 编写样式
- Axios 统一封装请求/响应拦截器

## 14. 开发方式

- 先输出数据库表结构设计和项目目录结构，等确认后再编码
- 一个模块一个模块地做，每个模块完成后暂停等确认
- 开发顺序：Phase 0 骨架 → Phase 1 预警引擎 → Phase 2 消息推送 → Phase 3 Redis缓存 → Phase 4 定时调度 → Phase 5 SOP工作流 → Phase 6 多库适配 → Phase 7 前端
- 每个 Phase 完成后流程：① compileJava 验证 ② 更新 PROGRESS.md 状态为"待审查" ③ 提交 Codex 审查（参考 agents.md，可能多轮） ④ 审查通过后更新 PROGRESS.md 为"✅ 完成"（补充关键设计决策） ⑤ git commit + push
