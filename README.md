## 🧪 Automated Testing

> All scripts are located in `scripts/`. Reports are output to `.codex-run/`.
> 所有脚本位于 `scripts/` 目录，测试报告输出到 `.codex-run/`。

### 1. Full E2E Test — `full-e2e-playwright.js`

Covers 10 modules: authentication, warning indexes (7 types), threshold config, custom SQL security, warning trigger chain, dictionary management, SOP designer, task management, and RBAC hardening.

覆盖 10 个模块：鉴权、预警指标（7类）、阈值配置、自定义 SQL 安全、预警触发链路、字典管理、SOP 设计器、任务管理、RBAC 加固。

```bash
node scripts/full-e2e-playwright.js
# Report: .codex-run/full-e2e-report.json
```

**Expected output:**
```json
{ "passed": 29, "failed": 0 }
```

---

### 2. Concurrency & Data Consistency — `warn-trigger-concurrency.ps1`

Fires 20 concurrent warning trigger requests and cross-validates that the DB record delta exactly matches the API-reported trigger count (business logic: 1 trigger → 2 records).

执行 20 并发预警触发请求，并交叉验证数据库记录增量与接口返回触发量严格一致（业务逻辑：1 次触发 → 2 条记录）。

```powershell
.\scripts\warn-trigger-concurrency.ps1
# Report: .codex-run/warn-trigger-concurrency-report.json
```

**Expected output:**
```json
{
  "successCount": 20,
  "failedCount": 0,
  "avgMs": 3227.07,
  "p95Ms": 3651.07,
  "actualWarnRecordDelta": 40,
  "sumTriggeredByApi": 40,
  "recordDeltaMatchesApiSum": true
}
```

---

### 3. RBAC Permission Isolation — `permission-isolation-check.ps1`

Validates that backend (not just frontend routing) enforces role boundaries with three-state assertion: `403` (forbidden), `200` (allowed), `401` (unauthenticated).

验证权限拦截发生在后端（而非仅前端路由），并做三态断言：`403`（禁止）、`200`（允许）、`401`（未登录）。

```powershell
.\scripts\permission-isolation-check.ps1
# Report: .codex-run/permission-isolation-report.json
```

**Expected output:**
```json
{ "total": 11, "passed": 11, "failed": 0 }
```

Key assertions:

| Endpoint | operator role | Expected |
|----------|--------------|----------|
| `/api/users` | ✗ | 403 |
| `/api/sop/workflows` | ✗ | 403 |
| `/api/sop/task-templates` | ✗ | 403 |
| `/api/warn/rules` | ✓ | 200 |
| `/api/sop/tasks` | ✓ | 200 |
| Any protected endpoint | anonymous | 401 |

---

### Prerequisites

```bash
# Frontend
cd mini-doamp-vue && npm install && npm run serve   # http://localhost:8090

# Backend
.\gradlew.bat :mini-doamp-server:bootRun            # http://localhost:9999

# Default test account: admin / admin123
```
