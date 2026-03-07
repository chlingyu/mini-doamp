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
