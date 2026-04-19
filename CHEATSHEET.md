# 新开对话小抄

## 对 Claude 说（写代码时）

```
继续 mini-doamp 项目开发，请先读 D:\Projects\mini-doamp\PROGRESS.md 确认当前进度。
```

## 对 Codex 说（审查时）

```
请先读以下两个文件，然后按当前 Phase 审查：
- D:\Projects\mini-doamp\PROGRESS.md
- D:\Projects\mini-doamp\agents.md
```

## 就这两句，不用记别的。

## 前后端联调启动命令（生产级，依赖 Docker Compose）

### 中间件（MySQL / Redis / RabbitMQ）

```powershell
cd D:\Projects\mini-doamp
docker compose up -d
```

### 后端

```powershell
cd D:\Projects\mini-doamp
$env:JWT_SECRET="miniDoampDevKey12345678901234567890"
.\gradlew.bat :mini-doamp-server:bootRun
```

- 访问地址：`http://localhost:9999`
- `bootRun` 已自动附加 `-Dfile.encoding=UTF-8`（Windows 中文编码修复）
- 直接运行 jar：`java -Dfile.encoding=UTF-8 -jar mini-doamp.jar`
- 若未启动 `XXL-Job Admin`，控制台会出现注册失败日志，但不影响大部分页面联调

### 前端

```powershell
cd D:\Projects\mini-doamp\mini-doamp-vue
npm run serve
```

- 访问地址：`http://localhost:8090/#/login`
- 已配置 `/api` 代理到 `http://localhost:9999`

### 默认账号

- 管理员：`admin / admin123`
- 运营员：`operator / admin123`
