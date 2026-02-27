# Mini DOAMP 技术选型说明

> 每项技术选择都附带面试话术，帮助回答"为什么选这个"类问题

## 1. 消息队列：RabbitMQ vs Kafka

### 选择：RabbitMQ

| 维度 | RabbitMQ | Kafka |
|------|----------|-------|
| 定位 | 消息代理（Broker） | 分布式事件流平台 |
| 消息模型 | 推模式，支持多种 Exchange | 拉模式，基于 Partition |
| 路由能力 | Topic/Direct/Fanout/Headers | 仅 Topic |
| 消息确认 | 支持 ACK/NACK/Reject | 基于 Offset 提交 |
| 死信队列 | 原生支持 DLX | 需自行实现 |
| 适用场景 | 业务消息、任务队列 | 日志采集、大数据流 |

### 面试话术
> "我们的预警消息量不大（日均几百到几千条），核心需求是可靠投递和灵活路由。RabbitMQ 的 Topic Exchange 天然支持按通知方式动态路由到不同队列，死信队列也是原生支持，不需要额外开发。Kafka 更适合高吞吐的日志场景，对我们来说过重了。"

## 2. 分布式锁：ShedLock vs Redisson

### 选择：ShedLock

| 维度 | ShedLock | Redisson |
|------|----------|----------|
| 定位 | 定时任务防重复执行 | 通用分布式锁框架 |
| 存储 | JDBC/Redis/MongoDB 等 | 仅 Redis |
| 集成方式 | 注解 `@SchedulerLock` | 编程式 `RLock` |
| 锁粒度 | 任务级别 | 任意粒度 |
| 复杂度 | 极低，几行配置 | 较高，需理解看门狗机制 |
| 适用场景 | 定时任务去重 | 通用分布式锁 |

### 面试话术
> "ShedLock 专门解决分布式环境下定时任务重复执行的问题，用注解就能搞定，存储用 MySQL 的 shedlock 表即可，不额外依赖 Redis。我们遇到的核心问题是不同任务共用同一个锁名导致互相抢锁，通过动态生成锁名称解决——预警检查用 `warn_check_${ruleId}`，SOP 任务生成用 `sop_generate_${templateId}`，按业务粒度隔离。Redisson 功能更全但对我们这个场景来说太重了。"

## 3. 定时调度：XXL-Job vs Spring @Scheduled vs Quartz

### 选择：XXL-Job

| 维度 | XXL-Job | @Scheduled | Quartz |
|------|---------|------------|--------|
| 管理界面 | 自带 Web 控制台 | 无 | 无（需自建） |
| 分布式支持 | 原生支持 | 不支持 | 支持但配置复杂 |
| 动态调度 | 支持动态修改 cron | 需重启 | 支持但 API 复杂 |
| 失败重试 | 内置 | 无 | 需自行实现 |
| 日志追踪 | 内置执行日志 | 无 | 无 |

### 面试话术
> "XXL-Job 提供了开箱即用的分布式调度能力和可视化管理界面，支持动态修改调度策略不需要重启应用。@Scheduled 只适合单机，Quartz 虽然支持分布式但配置繁琐且没有管理界面。我们配合 ShedLock 使用，XXL-Job 负责调度触发，ShedLock 负责防重复执行。"

## 4. ORM：MyBatis Plus vs JPA/Hibernate

### 选择：MyBatis Plus

| 维度 | MyBatis Plus | JPA/Hibernate |
|------|-------------|---------------|
| SQL 控制 | 完全掌控 SQL | 自动生成，复杂查询难优化 |
| 学习曲线 | 低（会 SQL 就行） | 高（需理解 JPQL、懒加载等） |
| 多表关联 | XML 灵活编写 | @OneToMany 等注解，N+1 问题 |
| 分页 | 内置分页插件 | 需额外配置 |
| 代码生成 | 内置代码生成器 | 无 |

### 面试话术
> "金融行业对 SQL 性能要求高，MyBatis Plus 让我们完全掌控 SQL，方便做慢查询优化。而且它的 databaseIdProvider 机制天然支持多数据库适配，我们基于这个做了 MySQL/H2 的方言切换。JPA 自动生成的 SQL 在复杂报表场景下很难优化。"

## 5. 缓存：Redis vs 本地缓存（Caffeine/Guava）

### 选择：Redis

| 维度 | Redis | Caffeine/Guava |
|------|-------|----------------|
| 分布式 | 天然支持多实例共享 | 仅单机 |
| 数据结构 | String/Hash/Set/List/ZSet | Map |
| 过期策略 | 支持 TTL | 支持 |
| 幂等去重 | SET 命令天然支持 | 不适合 |
| 持久化 | RDB/AOF | 无 |

### 面试话术
> "我们用 Redis 主要两个场景：一是缓存字典和高频指标数据，采用 Cache Aside 模式；二是消息幂等去重，用 SET 数据结构存消息ID。本地缓存在多实例部署时无法共享，而且我们的幂等消费必须跨实例生效，所以选 Redis。"

## 6. 工作流：自研状态机 vs Activiti/Flowable

### 选择：自研状态机

| 维度 | 自研状态机 | Activiti/Flowable |
|------|-----------|-------------------|
| 复杂度 | 低，代码可控 | 高，框架学习成本大 |
| 灵活性 | 完全自定义 | 受框架约束 |
| 表结构 | 自定义，简洁 | 框架自带 20+ 张表 |
| 回退机制 | 自行实现，灵活 | 原生不支持任意节点回退 |
| 面试价值 | 高，体现设计能力 | 低，只是会用框架 |

### 面试话术
> "我们的 SOP 流程相对固定（创建→分配→执行→审批→完成），不需要 Activiti 那种复杂的 BPMN 引擎。自研状态机让我们完全掌控状态转换逻辑，特别是回退到任意节点这个需求，Activiti 原生不支持，自研反而更灵活。而且面试时能讲清楚状态机的设计思路，比说'我用了 Activiti'更有说服力。"

## 7. 流程设计器：AntV X6 vs BPMN.js vs JointJS

### 选择：AntV X6

| 维度 | AntV X6 | BPMN.js | JointJS |
|------|---------|---------|---------|
| 生态 | 蚂蚁金服，中文文档完善 | Camunda 出品 | 独立开源 |
| 上手难度 | 低 | 中（BPMN 规范复杂） | 中 |
| 自定义节点 | 灵活，支持 Vue/React 渲染 | 受 BPMN 规范约束 | 灵活 |
| 与 Vue 集成 | 原生支持 | 需适配 | 需适配 |

### 面试话术
> "AntV X6 是蚂蚁金服的图编辑引擎，中文文档完善，和 Vue 集成方便。我们的流程不需要遵循 BPMN 规范，X6 的自定义节点能力足够满足需求，而且它支持只读模式，任务详情页直接复用设计器组件就能展示流程图并高亮当前节点。"

## 8. 认证：Spring Security + JWT vs Sa-Token vs Shiro

### 选择：Spring Security + JWT

| 维度 | Spring Security + JWT | Sa-Token | Shiro |
|------|----------------------|----------|-------|
| 生态 | Spring 官方，社区最大 | 国产轻量框架 | Apache，较老 |
| 无状态 | JWT 天然无状态 | 支持 | 需额外配置 |
| 过滤器链 | 完善，可扩展 | 简单 | 简单 |
| 面试认可度 | 最高 | 一般 | 一般 |

### 面试话术
> "Spring Security 是 Spring 生态的标准认证框架，配合 JWT 实现无状态认证，适合前后端分离架构。Token 存在前端 localStorage，每次请求通过 Axios 拦截器注入 Authorization 头，后端 Filter 校验签名和过期时间。"

> **追问预备（XSS 风险）**：localStorage 存 Token 确实有 XSS 风险，替代方案是用 httpOnly Cookie 存储，但会引入 CSRF 问题。我们项目前端做了 XSS 防护（不使用 v-html 渲染用户输入），权衡后选择 localStorage 方案更简单。如果安全要求更高，可以改用 httpOnly Cookie + CSRF Token 双重防护。

## 9. 构建工具：Gradle vs Maven

### 选择：Gradle

| 维度 | Gradle | Maven |
|------|--------|-------|
| 构建速度 | 增量编译，快 | 全量编译，慢 |
| 配置方式 | Groovy/Kotlin DSL | XML |
| 多模块管理 | 灵活，依赖管理简洁 | 可以但 XML 冗长 |
| 学习曲线 | 中 | 低 |

### 面试话术
> "Gradle 的多模块管理比 Maven 更灵活，依赖声明也更简洁。我们项目有 7 个子模块，用 Gradle 的 subprojects 统一管理公共依赖，各模块只声明自己特有的依赖，比 Maven 的 parent POM + modules 方式更清晰。"
