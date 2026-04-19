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
| 5 | SOP工作流（状态机、流程引擎、回退任意节点） | ✅ 完成 | compileJava通过，Codex 4轮审查通过 |
| 6 | 多库适配（Adapter工厂、databaseIdProvider） | ✅ 完成 | compileJava/classes通过；已从 demo 扩展到真实业务 SQL（预警趋势/指标类型汇总/任务日志原生分页）；H2 / MySQL 两套运行态均已实测通过 |
| 7 | 前端（Vue3、AntV X6、Vuex、Axios） | ✅ 完成 | 后端 compileJava通过，前端 npm run build通过，Codex 自审通过 |

## 最近更新（2026-03-08）

- **Bug 修复：H2 模式中文乱码**
  - 根因：Windows JVM 默认编码 GBK，导致 UTF-8 SQL 初始化文件被错误解码写入 H2
  - 修复 1：`application-h2.yml` 添加 `spring.sql.init.encoding: UTF-8`
  - 修复 2：`mini-doamp-server/build.gradle` 添加 `bootRun { jvmArgs = ['-Dfile.encoding=UTF-8'] }`
  - API 验证通过：`管理员`、`系统CPU使用率`、`交易金额`、`银行余额` 全部正确
  - 更新 `CHEATSHEET.md`：jar 启动需手动加 `-Dfile.encoding=UTF-8`
- 浏览器全功能测试：55 用例外的全页面手动巡检，除中文乱码外未发现其他 bug

## 审查记录（2026-03-30）

- 已确认启用“每个 Phase 审查后同步更新 PROGRESS.md”的机制（本节作为审查日志入口）
- Phase 1（预警引擎）Codex 审查结论：
  - ✅ 符合：策略模式 + 7种指标策略 + WarnEngine 路由 + 指标主表/阈值子表解耦 + 自定义SQL双校验
  - ⚠️ 改进点1：新增指标类型仍依赖 `IndexType` 枚举与校验链路变更，扩展并非仅新增策略类
  - ⚠️ 改进点2：自定义SQL执行建议补充超时/最大返回行数等资源保护（当前注入防护已达标）
  - ❌ 严重不符：无
- Phase 2（消息推送）Codex 审查结论：
  - ✅ 符合：Topic Exchange + 3消费队列（sms/email/wxwork）+ DLX/DLQ + Redis SETNX 幂等 + Mock发送
  - ✅ 符合：自动补偿重试上限 `MAX_RETRY=3`，超限转 `ALARM`
  - ⚠️ 改进点1：手动重试接口当前允许 `ALARM` 状态继续重试并递增 `retryCount`，建议与“最多3次”口径统一
  - ⚠️ 改进点2：消息表为“当前状态模型”（单行覆盖更新），若面试强调“完整状态流转留痕”，建议补充状态变更历史表
  - ❌ 严重不符：无
- Phase 3（Redis缓存）Codex 审查结论：
  - ✅ 符合：Cache Aside 读链路（缓存→DB→回填）与写链路（更新DB后删缓存）已实现
  - ✅ 符合：延迟双删（事务提交后删缓存 + 延迟500ms再删）、随机TTL防雪崩、空值短TTL防穿透、手动刷新接口、key前缀规范
  - ⚠️ 改进点1：存在缓存击穿并发窗口（热点key失效瞬间无互斥回源），建议加单飞/互斥锁或本地短暂合并
  - ⚠️ 改进点2：缓存相关集成测试偏轻（当前主要是Bean/常量级），建议补充真实读写链路与延迟双删时序测试
  - ❌ 严重不符：无
- Phase 4（定时调度）Codex 审查结论：
  - ✅ 符合：已集成 XXL-Job（`warnCheckHandler` / `sopTaskGenerateHandler` / `msgCompensationHandler`），未使用 `@Scheduled`
  - ✅ 符合：ShedLock 锁名动态生成（`warn_check_{ruleId}` / `sop_generate_{templateId}`），锁表已建
  - ✅ 符合：调度任务包含“预警检查 + SOP任务生成”
  - ⚠️ 改进点1：同一执行周期内当前为单线程串行遍历模板/规则，动态锁解决“误抢锁”但不等于单实例内并行执行；如面试强调并行吞吐建议补充并发执行策略
  - ⚠️ 改进点2：H2 测试主要验证接口可用性，未完整覆盖真实 XXL 调度触发路径（该点已有文档化说明）
  - ❌ 严重不符：无
- Phase 5（SOP工作流）Codex 审查结论：
  - ✅ 符合：状态机模式（`TaskStatus` 枚举 + 转换矩阵）与 `WorkflowEngine.advance()` 主链路完整
  - ✅ 符合：节点处理策略模式（PROCESS/APPROVE/COPY/BRANCH）已实现，回退支持任意已完成节点，`TaskExec` 中间记录标记 `ROLLED_BACK` 且不物理删除
  - ✅ 符合：操作流水表按状态变更写入（操作人、时间、动作、备注），并在状态变更后触发 MQ 通知（复用 `warn.exchange`）
  - ✅ 符合：前端流程设计器/查看器基于 AntV X6，查看器为只读模式并高亮当前节点
  - ⚠️ 改进点1：`SopNotifier` 成功日志模板占位符与参数数量不匹配，日志中的 taskId/msgId 可能错位显示
  - ⚠️ 改进点2：节点类型流转目前主要使用字符串常量而非 `NodeType` 枚举，建议收敛为强类型以降低拼写风险
  - ❌ 严重不符：无
- Phase 6（多库适配）Codex 审查结论：
  - ✅ 符合：`databaseIdProvider` 自动识别数据库类型（MySQL/H2）
  - ✅ 符合：`DatabaseAdapter` 抽象 + 工厂模式路由实现完整
  - ✅ 符合：至少3类方言差异示例已落地（日期函数、分页语法、字符串聚合），并有 databaseId 差异化 SQL
  - ✅ 符合：H2 运行态与适配器行为有集成测试覆盖（`DatabaseAdapterTest`）
  - ⚠️ 改进点1：MySQL 运行态目前主要依赖文档化与手工联调记录，建议补一条可自动执行的 MySQL profile 冒烟集成测试
  - ❌ 严重不符：无
- Phase 7（前端）Codex 审查结论：
  - ✅ 符合：组件命名整体为 PascalCase（页面与流程组件）
  - ✅ 符合：API 文件按模块组织（auth/user/system/warn/sop）
  - ✅ 符合：Vuex 管理全局登录态与权限（token/profile/permissions）
  - ✅ 符合：Axios 统一请求/响应拦截（JWT 注入、401刷新、403处理）
  - ✅ 符合：流程设计器/查看器均基于 AntV X6，查看器只读并高亮当前节点
  - ⚠️ 改进点1：建议补充前端自动化用例覆盖 token 过期刷新与 403 跳转路径，降低回归风险
  - ❌ 严重不符：无
- 整改复审彻底完成（2026-03-30，历经 5 轮深层 Review，针对 4个P1+1个P2 彻底攻克）：
  - ✅ **SopNotifier 日志修复**：占位符修复完全生效。
  - ✅ **NodeType 枚举收敛**：SOP 主链路硬编码全部替换生效。
  - ✅ **自定义SQL资源防护**：改用 `StatementCallback` 实现线程隔离级属性，防止并发串扰。
  - ✅ **热点缓存防并发击穿**：`DictService` / `IndexDataService` 的真实读链路已全面接入 `CacheService.getOrLoad()` 单飞锁。
  - ✅ **手动重试终极并发安全**：
    1. 去除包裹的 `@Transactional`，解决异常回滚阻断 ALARM 落库的核心逻辑。
    2. 从 `updateById()` 改成 `UpdateWrapper (CAS)` 精准条件更新，阻断并发下重复发起重试。
    3. 异常回写精准化，仅更新失败相关字段（不再回写过期的 `retry_count` 数据视图），辅以原状态为 `RETRYING` 的加固条件。
- 后续规则：Phase 2~7 审查结果继续在本节按日期追加，统一使用“✅⚠️❌ + 代码位置 + 修改建议”格式

## 最近更新（2026-03-07）

- Phase 6 已从“只有 DemoDialectMapper 演示”补强为“真实业务接口接入多库适配”
- 新增真实业务方言差异接口：`/api/warn/records/trend`、`/api/warn/indexes/type-summary`、`/api/system/job/log/native`
- 已验证 H2 下 3 个接口返回 `200`，其中日期函数差异、字符串聚合差异、分页语法差异均已实际跑通
- 前端 API 已补齐调用入口：`mini-doamp-vue/src/api/warn.js`、`mini-doamp-vue/src/api/system.js`
- 本轮验证结果：后端 `compileJava` / `classes` 通过，前端 `npm run build` 通过
- 已启动 `docker-compose.yml` 中的 MySQL / Redis / RabbitMQ 容器，端口 `3306/6379/5672` 已连通
- MySQL 运行态已补完：默认 profile 在 `10102` 端口启动成功，日志确认连接 MySQL / RabbitMQ 成功，登录接口与 `/api/warn/records/trend`、`/api/warn/indexes/type-summary`、`/api/system/job/log/native` 均返回 `200`

## 安全收口与策略差异化（2026-03-07 午）

- **P0-1 CustomSqlValidator 升级**：从正则黑名单升级为 6 层结构化白名单校验（基础检查 → 语法子集 → 表白名单 → 函数白名单 → 精确 value 别名 → 反注入结构）
- 新增错误码 `CUSTOM_SQL_TABLE_DENIED(2008)` / `CUSTOM_SQL_FUNC_DENIED(2009)`
- 新增 `CustomSqlValidatorTest` 25 个单元测试，GPT 2 轮审查通过
- **P0-2 敏感配置迁移**：`application.yml` 中 10 个敏感值改为 `${ENV_VAR:default}` 占位符，`JWT_SECRET` 无默认值强制外部注入
- 新增 `.env.example` 文档化所有环境变量，`.gitignore` 覆盖 `.env` + `.env.*`
- **P1-1 Refresh Token Rotation**：`AuthService.refresh()` 生成新 token 对后立即拉黑旧 refresh token，防止重放攻击
- **P1-2 策略差异化增强**：7 种策略各有独立查询逻辑
  - Running：最新值 + 7 期滑动均值趋势
  - Bank：逐家银行对比 + 超限占比统计
  - Channel：绝对值 + 日环比变化率 ±20%
  - Employee：floor>=10 过滤 + 匿名记录跳过
  - Branch：全部营业部算术均值对比
  - 删除无用的 `AbstractGroupWarnStrategy`
- **最终抛光**：ChannelWarnStrategy 趋势记录语义修正（changeRate 作为 currentValue），H2Adapter.groupConcat() 改为 LISTAGG
- **GPT 总审通过**：简历第 1/2/3/4/5/7 条均已确认成立，无硬伤

## 关键设计决策（跨文件已统一）

- 延迟双删：更新DB → 删缓存 → 延迟500ms → 再删缓存
- 回退：不产生新终态，任务回到 EXECUTING，中间 TaskExec 标记 ROLLED_BACK（不物理删除）
- ShedLock 锁名双粒度：warn_check_${ruleId} / sop_generate_${templateId}
- 流程保存：API 用 nodeCode，后端插入后映射为 nodeId
- 消息状态枚举：PENDING/SENT/FAILED/RETRYING/ALARM
- TaskStatus 终态：COMPLETED / REJECTED / TERMINATED（无"已回退"）
- JWT 双 token：access（24h）+ refresh（7d），Filter 显式校验 type=access，refresh 端点用 DB 最新用户名签发
- refresh token rotation：刷新后旧 refresh token 立即拉黑（Redis SETEX + 剩余 TTL 自动过期），防止重放攻击
- 用户 DTO 拆分：UserCreateRequest（username+password 必填）/ UserUpdateRequest（无 username）
- 删除部门/角色前校验关联用户和子部门，创建/更新用户校验 deptId/roleId 存在性
- 预警引擎：Map<IndexType, WarnStrategy> 路由，无 if-else；7种策略独立实现类，各有独立查询与判定逻辑
- 自定义 SQL 6 层白名单校验：基础检查 → 语法子集 → 表白名单(6张) → 函数白名单(22个) → 精确 value 别名 → UNION/子查询/INTO 禁止，保存前+执行前双校验
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
- SopTaskGenerateJob 已实现模板扫描+动态锁 sop_generate_${templateId}，任务实例创建已对接
- SOP 工作流：WorkflowEngine.advance() 状态机校验 + 策略模式(NodeHandler) + 分支条件路由
- 状态转换矩阵含 EXECUTING/APPROVING 自循环，支持多处理节点串行推进
- 回退机制：rollback() 校验目标节点有 DONE 记录，中间 exec 标记 ROLLED_BACK，任务回到 EXECUTING
- SopNotifier：先落 MsgRecord(PENDING) 再投 msgId 到 MQ，与 Phase 2 消费者协议兼容
- handleReject fromStatus 取实时任务状态，terminate 补 MQ 通知
- 多库适配：databaseIdProvider(MySQL→mysql, H2→h2) + DatabaseAdapter接口 + 工厂模式路由
- 方言差异3项：dateFormat(DATE_FORMAT vs FORMATDATETIME)、paginate(参数顺序)、groupConcat(SEPARATOR)
- Mapper XML 用 databaseId 属性编写差异化 SQL，MyBatis 自动选择匹配语句
- 多库适配已落到真实业务 SQL：预警趋势按日期聚合、指标类型汇总、任务日志原生分页不再只是 demo 演示
- H2 聚合函数采用 `LISTAGG(column, ',')`，MySQL 采用 `GROUP_CONCAT(... SEPARATOR ', ')`，DatabaseAdapter 与 Mapper XML 已对齐
- 多库适配联调结果：H2 与 MySQL（Docker）两套运行态均已验证通过；默认 profile 识别数据库类型为 `MYSQL`
- MybatisPlusConfig 动态检测 DbType（不再硬编码 MYSQL），PaginationInnerInterceptor 适配
- H2 profile：auto-startup=false 关闭消费者（不排除 Rabbit 自动配置，Producer try-catch 容错）
- schema-h2.sql：移除 ENGINE/COMMENT/ON UPDATE，JSON→TEXT，TIMESTAMP(3)→TIMESTAMP
- republish() 补 try-catch 回写 FAILED 状态，与 publish() 容错一致
- 前端：Vue CLI 5 + Vue 3 + Vuex 4 + Vue Router 4 + Ant Design Vue 3 + AntV X6
- 请求层：Axios 统一封装，请求头注入 Bearer Token，401 自动 refresh，403 自动跳转工作台
- 登录态：localStorage 持久化 access/refresh token，Vuex 恢复用户信息与权限映射
- 流程设计器：FlowDesigner 基于 AntV X6 拖拽编辑，FlowViewer 只读渲染并高亮当前节点
- 认证接口补齐：新增 /api/auth/userInfo 与 /api/auth/logout 支撑前端登录态闭环
- 权限控制：前端不再硬编码角色权限，改为后端登录/刷新/userInfo 接口统一下发 permissions 集合；SOP 路由与页面按 `sop.task` / `sop.approve` 区分处理权限
- 权限映射：后端由 `security.permission` 配置项集中维护角色→权限映射，不再在代码里通过 roleCode 关键字猜测权限
- 登出撤销：logout 接口接收 refreshToken，基于 Redis 黑名单撤销 access/refresh token，refresh 与鉴权过滤器均校验黑名单
- JWT 配置：`jwt.secret` / `jwt.expiration` / `jwt.refresh-expiration` 改为必须显式配置，避免代码内默认密钥进入运行环境
- 联调验证：已实测 `--spring.profiles.active=h2` 可启动成功，Tomcat 监听 `9999`，H2 控制台可用；未启动 XXL-Job Admin 时会有注册失败日志，但不阻塞大部分页面联调

## 工作流程

Claude 写代码 → compileJava → 标记"待审查" → Codex 审查（可能多轮）→ 标记"✅ 完成" → git commit + push

## 最近更新（2026-03-30）- Codex 接管补充

- Phase 6 自动化冒烟补齐并复核通过：
  - `MysqlDialectSmokeTest` 容器镜像从 `mysql:8.0.33` 调整为 `mysql:8.0`（与本地/CI常见缓存一致，避免拉镜像阻塞导致“假卡死”）
  - 修正分页断言字段：`list` -> `records`（与 `PageResponse` 结构一致）
  - 实测命令：`./gradlew --no-daemon :mini-doamp-server:test --tests com.demo.minidoamp.integration.MysqlDialectSmokeTest`
  - 结果：`BUILD SUCCESSFUL`（1/1 通过）
- Phase 7 前端自动化补齐并通过：
  - 新增 Jest 测试基线：`mini-doamp-vue/jest.config.js`
  - 新增脚本：`mini-doamp-vue/package.json` -> `test:unit`
  - 新增用例：`mini-doamp-vue/tests/unit/request.spec.js`
    - 用例1：401 触发 refresh token 并重试原请求
    - 用例2：403 触发无权限提示并跳转 `#/dashboard`
  - 实测命令：`npm run test:unit`
  - 结果：`2 passed, 0 failed`
- 结论：Phase 6/7 的“可自动执行验证”缺口已关闭，可进入下一轮全量回归或面试演示准备。

---

## 生产化改造阶段（2026-04-18 启动）

面试级 → 生产级路线确认：K8s 自建 + 真微服务 + 前端全量 TS。Roadmap P-1→P7 共 9 阶段，预计 12-14 周。

### P-1：Java / Spring Boot 升级（✅ 完成，2026-04-19 合入 master，PR #1）

**目标**：Java 1.8 → 21、Spring Boot 2.7 → 3.4、删 H2、javax → jakarta。

| Step | 内容 | 状态 |
|---|---|---|
| 1 | Gradle 7.6 → 8.10.2 + Foojay Toolchain + Daemon JVM 切 JDK 17 | ✅ |
| 2 | Spring Boot 3.4.1 + Spring Cloud 2024.0.0 + MyBatis Plus 3.5.9 + jsqlparser + jjwt 0.12.6 + ShedLock 5.16 + MySQL Connector-J 8.4 + Testcontainers 1.20 | ✅ |
| 3 | javax → jakarta 全量替换（31 文件 53 import，validation/servlet/annotation.PostConstruct；保留 javax.sql / javax.crypto 属 JDK 内置） | ✅ |
| 4 | JwtUtil 改 jjwt 0.12 API + SecurityConfig 改 Spring Security 6（SecurityFilterChain lambda DSL + requestMatchers + authorizeHttpRequests） | ✅ |
| 5 | compileJava + compileTestJava 全模块 BUILD SUCCESSFUL | ✅ |
| 6 | 彻底删除 H2：H2Adapter.java / application-h2.yml / schema-h2.sql / DatabaseAdapterTest.java / Mapper XML databaseId="h2" 分支 / MybatisPlusConfig H2 检测；测试基建改 Testcontainers MySQL 共享单例 | ✅ |
| 7 | `bootRun` 冒烟 + docker-compose MySQL/Redis/RabbitMQ 全链路（9/9 端点 HTTP 200） | ✅ |
| 8 | `./gradlew test` 集成测试 48/48 green（server 16 + event 32，Testcontainers MySQL 8.0） | ✅ |
| 9 | 4-layer commit + /review APPROVE + /security-review zero findings + PR #1 merged | ✅ |
| 10 | 切 JDK 17 → JDK 21 LTS（Temurin 21.0.10+7 清华镜像手动下载，Foojay 在 GFW 下 connection-reset） | ✅ |

**净改动**：55 files changed, +238 insertions, -714 deletions。

**关键设计决策（P-1）**：
- Daemon JVM 用本地 JDK 17（`D:/jdk/jdk-17.0.8`，SB 3.4 Gradle plugin 最低要求），toolchain 编译目标用 JDK 21 LTS
- Foojay auto-download 在国内网络 connection-reset；改 `org.gradle.java.installations.paths` 显式列本地 JDK 目录 + `auto-download=false`
- MyBatis Plus 3.5.9 起 `PaginationInnerInterceptor` 依赖 jsqlparser，独立 artifact `mybatis-plus-jsqlparser` 需显式引入
- MySQL Connector artifact 从 `mysql:mysql-connector-java` → `com.mysql:mysql-connector-j`
- H2 作为多库适配的"演示方"彻底移除；`DatabaseAdapter` 接口 + Factory 保留（未来加 PostgreSQL/Oracle 可直接扩）
- 测试基建：`BaseIntegrationTest` 静态 Testcontainers MySQL 8.0（withReuse=true），所有继承类共享同一个容器实例降低启动代价
- Spring Framework 6 要求保留方法参数名（按名 bean 注入 / `@ConfigurationProperties` 绑定），全模块添加 `javac -parameters` 标志

### P0：可观测性与部署底座（worktree `upgrade-p0-observability`，🔄 冒烟通过，待 review/PR）

**目标**：结构化日志 + MDC/TraceId、Prometheus/Grafana、Flyway、SpringDoc、Docker 多阶段、GitHub Actions、Helm 骨架。

| 子阶段 | 内容 | 状态 |
|---|---|---|
| P0-a | 结构化日志：log4j2 console + JsonTemplateLayout(Logstash V1) + `MdcRequestFilter`(requestId/clientIp) + `JwtAuthFilter` 注入 userId + Micrometer Tracing (OTel bridge) 自动注入 traceId/spanId | ✅ |
| P0-b | Actuator + Micrometer + Prometheus Registry + 业务埋点(`warn.check.duration` / `warn.triggered.total` / `notify.publish.total` / `sop.task.advance.total/.duration`) + Grafana Dashboard JSON + Prometheus scrape config + compose stack(prometheus:9090, grafana:3000) | ✅ |
| P0-c | Flyway 迁移：`V1__init_schema.sql`(313 行) + `V2__init_data.sql`(幂等 `INSERT IGNORE` 含显式 id) + `baseline-on-migrate=true` + `BaseIntegrationTest` 去掉 `spring.sql.init`，测试改由 Flyway 驱动 + 旧 `src/main/resources/sql/` 彻底删除 | ✅ |
| P0-d | SpringDoc OpenAPI 3 (`springdoc-openapi-starter-webmvc-ui:2.6.0`) + `OpenApiConfig` 全局 Info/Contact/License + `bearer-jwt` Security Scheme + `SecurityConfig` 放行 `/v3/api-docs/**` + `/swagger-ui/**` + `/actuator/{health,info,prometheus}` | ✅ |
| P0-e | 多阶段 `Dockerfile`(JDK 21 builder + JRE 21 jammy runtime) + Gradle 依赖 layer 缓存 + Spring Boot Layered Jar(`java -Djarmode=layertools extract`) + 非 root(uid 1000) + HEALTHCHECK 走 `/actuator/health` + `.dockerignore` + docker-compose `--profile full` 里的 `app` 服务 | ✅ |
| P0-f | `.github/workflows/ci.yml`(JDK 21 + gradle test + Docker Buildx + ghcr.io push, cache-from/to type=gha) + `deploy/helm/mini-doamp`(Chart/values/_helpers/deployment/service/sa/configmap/hpa/ingress/servicemonitor + README) | ✅ |
| 冒烟 | `./gradlew test` 全绿（48 测试，Flyway 驱动 Testcontainers MySQL）+ `bootRun` 启动 5s + `/actuator/health=UP` + `/actuator/prometheus` 输出 Micrometer 指标 + `/v3/api-docs` 返回 OpenAPI 3.0.1 JSON + 日志里 `[traceId=... spanId=...] [req=... user=1 ip=...]` 全字段齐活 | ✅ |

**关键设计决策（P0）**：
- **日志框架**：保留 Log4j2（而非回 Logback），借 `log4j-layout-template-json` 拿到 Logstash V1 schema；`%X{k}` 读 Slf4j MDC（由 `log4j-slf4j2-impl` 自动桥接到 ThreadContext）。Log4j2 不支持 Logback 的 `%X{k:-default}` 语法，改为 `%X{k}`（缺失时渲染为空串）。
- **MDC 注入次序**：`MdcRequestFilter` 设 `Ordered.HIGHEST_PRECEDENCE` + `FilterRegistrationBean` 显式注册，保证在 SecurityFilterChain 之前跑；`JwtAuthFilter` 解出 userId 后**立刻** put MDC，避免 `PermissionService` 内部的 SQL 日志 `user=` 为空。
- **Tracing**：选 `micrometer-tracing-bridge-otel`（不是 brave），对齐 P2 Jaeger/Tempo；`probability=1.0` 本地全采样，生产用 `TRACING_SAMPLING` 环境变量覆盖到 0.1。
- **业务埋点命名**：`<domain>.<event>.{total,duration}` + 低基数 tag（`indexType` / `channel` / `action` / `nodeType` / `outcome`）；`ruleId` / `userId` 不上标签避免 cardinality 爆炸。
- **Flyway 幂等**：V2 所有 `INSERT` 全改 `INSERT IGNORE` + 显式 id，保证老库 baseline 首迁移 + 新库空库都能跑过。
- **Actuator 放行**：`/actuator/{health,info,prometheus}` 走 `permitAll()`；P2 会改成独立 management port + IP 白名单。
- **Dockerfile 分层**：先 COPY 各模块 `build.gradle` 预热依赖缓存，再 COPY 源码；Spring Boot Layered Jar 让依赖层（大部分不变）缓存命中率最大化；运行时用 `eclipse-temurin:21-jre-jammy` 瘦身。
- **Helm Chart 骨架**：非 root、滚动更新 `maxSurge=1/maxUnavailable=0`、liveness/readiness 拆到 `/actuator/health/{liveness,readiness}`、HPA 基于 CPU+Memory、可选 `ServiceMonitor`(Prometheus Operator)；Secret 接口统一为 `secretRef.name`（P1 由 External Secrets + Vault 供给）。
- **CI 触发**：PR 只跑 test，master 推送才 build+push 镜像到 `ghcr.io/${owner}/mini-doamp`；Buildx 缓存走 `type=gha`。

**遗留 / 下一步**：
- SpringDoc Controller 级 `@Tag` / `@Operation` 注解精细化 → P7（运维文档阶段）。
- Actuator `/actuator/prometheus` 生产级 IP 白名单 + management port 独立化 → P2。
- XXL-Job admin 在本地 dev 没启动会刷 `Connection refused` 错误日志，不影响 bootRun 冒烟；后续考虑 dev profile 里 `xxl.job.admin.addresses=""` 屏蔽。

