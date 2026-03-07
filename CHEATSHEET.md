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

## 明天前后端联调启动命令

### 后端（推荐先用 H2）

```powershell
cd D:\Projects\mini-doamp
$env:JWT_SECRET="miniDoampDevKey12345678901234567890"
.\gradlew.bat :mini-doamp-server:bootRun --args="--spring.profiles.active=h2"
```

- 访问地址：`http://localhost:9999`
- H2 控制台：`http://localhost:9999/h2-console`
- 已验证可启动；若未启动 `XXL-Job Admin`，控制台会出现注册失败日志，但不影响大部分页面联调

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
