# Mini DOAMP 架构设计文档

## 1. 整体架构图

```
┌─────────────────────────────────────────────────────┐
│                    用户浏览器                          │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP
                       ▼
┌─────────────────────────────────────────────────────┐
│              前端 (mini-doamp-vue)                    │
│  Vue 3 + Ant Design Vue + ECharts + AntV X6         │
│  端口: 8090                                          │
└──────────────────────┬──────────────────────────────┘
                       │ REST API (/api 代理)
                       ▼
┌─────────────────────────────────────────────────────┐
│              后端 (mini-doamp-server)                 │
│  Spring Boot 2.7 + Spring Security + JWT            │
│  端口: 9999                                          │
├─────────────────────────────────────────────────────┤
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │
│  │ gateway │ │  doamp  │ │  event  │ │   sop   │   │
│  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘   │
│       └───────────┴───────────┴───────────┘         │
│                       │                              │
│              ┌────────┴────────┐                     │
│              │   core + api    │                     │
│              └─────────────────┘                     │
└──────┬──────────────┬──────────────┬────────────────┘
       │              │              │
       ▼              ▼              ▼
  ┌─────────┐   ┌─────────┐   ┌─────────┐
  │ MySQL   │   │  Redis  │   │RabbitMQ │
  │ :3306   │   │ :6379   │   │ :5672   │
  └─────────┘   └─────────┘   └─────────┘
```

## 2. Gradle 模块拆分与职责

### 2.1 模块总览

| 模块 | 类型 | 职责 |
|------|------|------|
| mini-doamp-api | 纯定义 | DTO、VO、常量、错误码，无业务逻辑 |
| mini-doamp-core | 基础层 | 实体类、Mapper、工具类、枚举、数据库适配器 |
| mini-doamp-gateway | 业务层 | Spring Security + JWT 认证、用户管理、文件服务 |
| mini-doamp-doamp | 业务层 | 指标查询、字典管理、Redis 缓存服务 |
| mini-doamp-event | 业务层 | 预警引擎、策略路由、RabbitMQ 消息推送 |
| mini-doamp-sop | 业务层 | 工作流引擎、任务管理、流程定义、定时调度 |
| mini-doamp-server | 启动层 | Application 入口、配置文件、日志配置 |

### 2.2 模块依赖关系

```
                    server
                   /  |  \  \
                  /   |   \   \
            gateway doamp event sop
                \    |    /   /
                 \   |   /   /
                   core
                    |
                   api
```

依赖规则：
- api 是最底层，被所有模块引用，只放 DTO/VO/常量
- core 依赖 api，放实体类、Mapper、工具类
- 四个业务模块（gateway/doamp/event/sop）依赖 core + api
- 业务模块之间不直接依赖，通过 core 中的公共接口解耦
- server 依赖所有业务模块，负责组装启动

## 3. 分层架构

每个业务模块内部遵循统一的三层架构：

```
Controller（控制层）
    │  参数校验、调用Service、不写业务逻辑
    ▼
Service（业务层）
    │  业务逻辑、事务管理、调用DAO/Mapper
    ▼
DAO / Mapper（数据层）
    │  数据库操作、SQL映射
    ▼
Database
```

### 数据流转路径

```
前端请求 → Controller → Service → Mapper/DAO → DB
                                      ↕
                                    Redis（缓存层）
                                      ↕
                                   RabbitMQ（异步消息）
```

### 对象转换链路

```
Request DTO → Entity（DB操作）→ Response VO（返回前端）
         ↑                           ↑
         └── MapStruct 自动转换 ──────┘
```

## 4. 核心设计模式

### 4.1 策略模式（预警引擎）

```
WarnStrategy（接口）
    │
    ├── RunningWarnStrategy      运行类指标
    ├── OperationWarnStrategy    运营类指标
    ├── BankWarnStrategy         银行类指标
    ├── ChannelWarnStrategy      渠道效能类指标
    ├── EmployeeWarnStrategy     员工类指标
    ├── BranchWarnStrategy       营业部类指标
    └── CustomSqlWarnStrategy    自定义SQL类指标

WarnEngine（引擎）
    → 接收规则ID
    → 查关联指标和阈值
    → 从 Map<IndexType, WarnStrategy> 路由到对应策略
    → 执行查询 + 对比阈值
    → 生成预警记录
```

扩展方式：新增指标类型只需新增策略实现类 + 注册到 Map，无需改动引擎代码。

### 4.2 状态机模式（SOP 工作流）

```
状态枚举 TaskStatus：
  CREATED → PENDING_ASSIGN → EXECUTING → APPROVING → COMPLETED
                                                   → REJECTED
                                                   → TERMINATED

状态转换矩阵（允许的转换）：
  CREATED        → PENDING_ASSIGN
  PENDING_ASSIGN → EXECUTING
  EXECUTING      → APPROVING, TERMINATED
  APPROVING      → COMPLETED, REJECTED, EXECUTING（回退）
  REJECTED       → EXECUTING（重新执行）
```

节点处理策略：
- ProcessNodeHandler：记录执行结果
- ApproveNodeHandler：记录审批意见
- CopyNodeHandler：发送通知（复用 RabbitMQ）
- BranchNodeHandler：评估条件选择分支

### 4.3 适配器工厂模式（多库适配）

```
DatabaseAdapter（抽象接口）
    │  dateFormat(column, pattern)
    │  paginate(sql, offset, size)
    │  groupConcat(column)
    │
    ├── MysqlAdapter
    └── H2Adapter

DatabaseAdapterFactory
    → 读取 databaseIdProvider 识别的数据库类型
    → 返回对应 Adapter 实例
```

### 4.4 消息异步架构（RabbitMQ）

```
预警引擎触发
    │
    ▼
WarnMessageProducer
    │  发送到 Topic Exchange (warn.exchange)
    │  routing key: warn.sms / warn.email / warn.wxwork
    │
    ├──→ sms.queue ──→ SmsConsumer（Mock打印日志）
    ├──→ email.queue ──→ EmailConsumer（Mock打印日志）
    └──→ wxwork.queue ──→ WxWorkConsumer（Mock打印日志）
              │
              │ 消费失败
              ▼
         死信队列 (DLQ)
              │
              ▼
         定时任务扫描补偿重试（最多3次）
              │
              │ 超限
              ▼
         标记"告警"状态，人工处理
```

## 5. 前端架构

### 5.1 目录结构

```
mini-doamp-vue/
├── src/
│   ├── api/              # 接口模块
│   │   ├── warn.js       # 预警接口
│   │   ├── sop.js        # SOP接口
│   │   ├── system.js     # 系统接口
│   │   └── user.js       # 用户接口
│   ├── components/       # 公共组件
│   │   └── FlowDesign/   # 流程设计器组件（AntV X6）
│   ├── router/           # 路由配置
│   ├── store/            # Vuex 状态管理
│   │   └── modules/
│   │       └── user.js   # 用户状态（token、权限）
│   ├── utils/            # 工具函数
│   │   ├── request.js    # Axios 封装
│   │   └── auth.js       # JWT token 管理
│   ├── views/            # 页面组件
│   │   ├── login/        # 登录
│   │   ├── warn/         # 预警管理
│   │   ├── sop/          # SOP工作流
│   │   └── system/       # 系统管理
│   ├── App.vue
│   └── main.js
├── vue.config.js
└── package.json
```

### 5.2 请求链路

```
页面组件 → api/xxx.js → utils/request.js（Axios）
                              │
                              ├── 请求拦截：注入 JWT token
                              ├── 响应拦截：统一错误处理
                              └── 403：自动跳转登录页
```

## 6. 缓存架构

```
请求到达
    │
    ▼
查 Redis 缓存 ──→ 命中 → 直接返回
    │
    │ 未命中
    ▼
查 MySQL ──→ 结果为空 → 缓存空值（TTL=5min）→ 返回空
    │
    │ 有数据
    ▼
回填 Redis（TTL = 30min ± random(5min)）→ 返回数据

更新数据时：
    更新DB → 删缓存 → sleep(500ms) → 再删缓存（延迟双删）
```

## 7. JWT 认证设计

- 登录签发 access token（有效期 24h）+ refresh token（有效期 7d）
- access token 用于业务接口鉴权，refresh token 仅用于 `/api/auth/refresh` 换发新 token 对
- JwtAuthFilter 校验 token 类型，拒绝 refresh token 访问业务接口
- 每个 token 携带 `jti`（JWT ID）字段，预留撤销扩展点

**生产环境扩展方案（当前未实现，面试口述）：**
- Redis 黑名单：用户登出或密码变更时，将旧 refresh token 的 jti 加入 Redis SET，refresh 时校验
- tokenVersion：用户表增加 version 字段，签发时写入 token，校验时比对，密码变更时 version+1 使所有旧 token 失效

