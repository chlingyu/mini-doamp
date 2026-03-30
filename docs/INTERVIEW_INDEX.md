# 面试证据索引

> 简历每条描述 → 代码入口 → 可演示 API → 集成测试

| # | 简历描述 | 核心代码入口 | 可演示 API | 集成测试 |
|---|---------|-------------|-----------|---------|
| 1 | **预警引擎** — 策略模式，7种指标类型 | `WarnEngine.java` → `WarnStrategy` 接口 + 7 个实现 | `POST /api/warn/rules/{id}/trigger` | `WarnMqChainTest` |
| 2 | **消息推送** — Topic Exchange + 幂等 + 死信队列 | `WarnMessageProducer.java` + `RabbitMqConfig.java` + `MsgConsumer.java` | `GET /api/msg/records` | `WarnMqChainTest` |
| 3 | **Redis缓存** — Cache Aside + 延迟双删 + 防雪崩/穿透 | `CacheService.java` + `CacheConstants.java` | `POST /api/cache/refresh/all` | `CacheServiceTest` |
| 4 | **定时调度** — XXL-Job + ShedLock 动态锁名 | `WarnCheckJob.java` + `SopTaskGenerateJob.java` | `POST http://localhost:9998/run` | `ShedLockJobTest` |
| 5 | **多库适配** — databaseIdProvider + Adapter 工厂 | `DatabaseAdapterFactory.java` + `H2Adapter.java` + `MySqlAdapter.java` | `GET /api/db/info` | `DatabaseAdapterTest` |
| 6 | **慢查询优化** | 口述项，不审查代码 | — | — |
| 7 | **SOP工作流** — 状态机 + 审批回退 + AntV X6 | `WorkflowEngine.java` + `NodeHandler` 策略 | `POST /api/sop/task-execs/{id}/advance` | `SopNotificationTest` |
| 8 | **gRPC通信** | 口述项，不审查代码 | — | — |

## 关键演示命令

### 启动 H2 模式（无需 Docker）
```powershell
$env:JWT_SECRET="miniDoampDevKey12345678901234567890"
.\gradlew.bat :mini-doamp-server:bootRun --args="--spring.profiles.active=h2"
```

### 启动 MySQL 模式（需要 Docker）
```powershell
docker-compose up -d
$env:JWT_SECRET="miniDoampDevKey12345678901234567890"
$env:DB_URL="jdbc:mysql://localhost:3306/mini_doamp?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
.\gradlew.bat :mini-doamp-server:bootRun
```

### 运行全部测试
```powershell
$env:JWT_SECRET="miniDoampDevKey12345678901234567890"
cmd /c "gradlew.bat test -Dfile.encoding=UTF-8"
```

### L4 全栈验证
```powershell
.\scripts\verify-l4.ps1
```

## 安全演示点
| 功能 | 演示方法 |
|------|---------|
| JWT Rotation | 登录→刷新→旧 refreshToken 重放被拒 |
| SQL 注入防护 | 自定义 SQL 指标输入 `DROP TABLE` 被拦截 |
| 权限校验 | 不带 Token 调用 `/api/warn/indexes` 返回 401 |
