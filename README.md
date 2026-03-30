# Mini DOAMP

> 运营分析预警平台 — Operations Analysis & Warning Platform

## Quick Start

### Option A — Zero-dependency (H2 mode, recommended for demo)

No Docker, no MySQL, no RabbitMQ. Everything runs in-memory.

```bash
# 1. Backend (H2 in-memory, port 9999)
./gradlew.bat :mini-doamp-server:bootRun -Pargs='--spring.profiles.active=h2'

# 2. Frontend (port 8090)
cd mini-doamp-vue && npm install && npm run serve
```

Open http://localhost:8090 — Login: `admin` / `admin123`

> **Note:** H2 mode disables RabbitMQ consumers. MQ-related features (message delivery, DLQ retry) will mock-send only. All other features work identically.

---

### Option B — Full Stack (MySQL + Redis + RabbitMQ via Docker)

```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Backend (MySQL mode, port 9999)
set DB_PASSWORD=root
set JWT_SECRET=your_jwt_secret_at_least_32_chars_here
./gradlew.bat :mini-doamp-server:bootRun

# 3. Frontend (port 8090)
cd mini-doamp-vue && npm install && npm run serve
```

> **Tip:** See `.env.example` for all configurable environment variables.

---

## 🧪 Automated Testing

> All scripts are located in `scripts/`. Reports are output to `.codex-run/`.
> 所有脚本位于 `scripts/` 目录，测试报告输出到 `.codex-run/`。

### Prerequisites

Start both backend (:9999) and frontend (:8090) using either Option A or B above, then run:

```bash
# Full E2E (29 cases, Playwright)
node scripts/full-e2e-playwright.js

# Concurrency & data integrity (20 concurrent triggers + baseline + SLA)
powershell -File scripts/warn-trigger-concurrency.ps1

# RBAC permission isolation (11 three-state assertions)
powershell -File scripts/permission-isolation-check.ps1

# Resilience & recovery (3 failure-mode scenarios)
powershell -File scripts/resilience-check.ps1
```

All reports output to `.codex-run/` as JSON.

---

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

Fires 20 concurrent warning trigger requests, measures baseline single-request latency, and cross-validates that the DB record delta exactly matches the API-reported trigger count (business logic: 1 trigger → 2 records).

执行 20 并发预警触发请求，先测量单请求基线延迟，再交叉验证数据库记录增量与接口返回触发量严格一致（业务逻辑：1 次触发 → 2 条记录）。

```powershell
.\scripts\warn-trigger-concurrency.ps1
# Report: .codex-run/warn-trigger-concurrency-report.json
```

| Metric | Value | Note |
|--------|-------|------|
| Concurrency | 20 | PowerShell `Start-Job` parallel HTTP |
| Baseline (single) | ~160 ms | 3-run median, cold JVM |
| P95 (20 conc) | ~3651 ms | Expected degradation under H2 + single-thread |
| **SLA target** | ≤ 5000 ms | Internal business rule; **passed with ~37% headroom** |
| Data integrity | ✅ | `recordDeltaMatchesApiSum = true` — 20 × 2 = 40, exact match |

> **Architecture note**: The test validates **data consistency** (zero loss, zero duplication) under concurrency, not raw throughput. Production deployments with MySQL connection pooling and multi-instance scaling would show significantly lower degradation ratios.

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

### 4. Resilience & Recovery — `resilience-check.ps1`

Validates the message compensation pipeline handles three failure modes: failed delivery auto-retry, idempotent deduplication under concurrency, and retry exhaustion escalation to ALARM.

验证消息补偿管道处理三种故障模式：失败消息自动重试、并发场景幂等去重、重试耗尽升级为人工告警。

```powershell
.\scripts\resilience-check.ps1
# Report: .codex-run/resilience-check-report.json
```

| Scenario | Description | Verified |
|----------|-------------|----------|
| S1: Compensation | Message FAILED → retry via compensation job → status updates | ✅ |
| S2: Idempotent | 3 concurrent retries → CAS ensures at most 1 succeeds | ✅ |
| S3: ALARM escalation | Retry ≥ 3 times → status escalates to ALARM | ✅ |

---

## Architecture

See `docs/` for detailed documentation:
- `docs/architecture-design.md` — System architecture & module design
- `docs/database-design.md` — Database schema
- `docs/api-design.md` — 60+ REST API endpoints
- `docs/development-phases.md` — Phase 0–7 development plan
- `docs/tech-selection.md` — Technology stack rationale
- `docs/interview-qa.md` — Interview Q&A preparation

## Test Accounts

| Username | Password | Role | Notes |
|----------|----------|------|-------|
| `admin` | `admin123` | Admin | Full access to all modules |
| `operator` | `admin123` | Operator | Warning + task access only; restricted from user/workflow management |
