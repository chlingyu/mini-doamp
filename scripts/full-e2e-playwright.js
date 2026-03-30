const { chromium } = require("playwright");
const fs = require("fs");
const path = require("path");

const BASE_URL = process.env.E2E_BASE_URL || "http://localhost:8090";
const API_BASE = process.env.E2E_API_BASE || "http://localhost:8090/api";
const USERNAME = process.env.E2E_USERNAME || "admin";
const PASSWORD = process.env.E2E_PASSWORD || "admin123";

const report = {
  startedAt: new Date().toISOString(),
  baseUrl: BASE_URL,
  apiBase: API_BASE,
  ui: {
    login: { ok: false, detail: "" },
    routes: [],
    actions: [],
    pageErrors: [],
    failedRequests: []
  },
  api: {
    checks: []
  },
  summary: {
    passed: 0,
    failed: 0
  }
};

function pushResult(bucket, name, ok, detail = "") {
  bucket.push({ name, ok, detail });
  if (ok) {
    report.summary.passed += 1;
  } else {
    report.summary.failed += 1;
  }
}

async function safeStep(bucket, name, fn) {
  try {
    const detail = await fn();
    pushResult(bucket, name, true, detail || "OK");
    return true;
  } catch (error) {
    pushResult(bucket, name, false, error?.message || String(error));
    return false;
  }
}

async function apiRequest(token, method, endpoint, body) {
  const res = await fetch(`${API_BASE}${endpoint}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`
    },
    body: body ? JSON.stringify(body) : undefined
  });
  const text = await res.text();
  let data;
  try {
    data = JSON.parse(text);
  } catch {
    data = text;
  }
  return { status: res.status, data };
}

async function run() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();

  page.on("pageerror", err => {
    report.ui.pageErrors.push(err.message || String(err));
  });
  page.on("response", response => {
    const status = response.status();
    if (status >= 400) {
      const url = response.url();
      if (!url.includes("sockjs-node")) {
        report.ui.failedRequests.push({ status, url });
      }
    }
  });

  const routes = [
    { name: "Dashboard", path: "/dashboard" },
    { name: "Warn Indexes", path: "/warn/indexes" },
    { name: "Warn Rules", path: "/warn/rules" },
    { name: "Warn Records", path: "/warn/records" },
    { name: "Message Records", path: "/warn/messages" },
    { name: "SOP Workflows", path: "/sop/workflows" },
    { name: "SOP Templates", path: "/sop/templates" },
    { name: "SOP Tasks", path: "/sop/tasks" },
    { name: "System Dicts", path: "/system/dicts" },
    { name: "System Jobs", path: "/system/jobs" }
  ];

  try {
    await page.goto(`${BASE_URL}/#/login`, { waitUntil: "domcontentloaded" });
    await page.waitForSelector("input", { timeout: 15000 });
    const allInputs = page.locator("input");
    await allInputs.nth(0).fill(USERNAME);
    await allInputs.nth(1).fill(PASSWORD);
    await page.locator("button[type='submit']").click();
    await page.waitForURL(/#\/dashboard/, { timeout: 15000 });
    report.ui.login.ok = true;
    report.ui.login.detail = "Login succeeded and redirected to /dashboard";
    report.summary.passed += 1;
  } catch (error) {
    report.ui.login.ok = false;
    report.ui.login.detail = error?.message || String(error);
    report.summary.failed += 1;
  }

  for (const route of routes) {
    await safeStep(report.ui.routes, `Route ${route.name}`, async () => {
      await page.goto(`${BASE_URL}/#${route.path}`, { waitUntil: "domcontentloaded" });
      await page.waitForSelector(".layout-header-title", { timeout: 15000 });
      const currentUrl = page.url();
      if (currentUrl.includes("/login")) {
        throw new Error(`Redirected to login on ${route.path}`);
      }
      const header = await page.locator(".layout-header-title").first().innerText();
      await page.waitForTimeout(500);
      return `Header: ${header}`;
    });
  }

  await safeStep(report.ui.actions, "Warn rules manual trigger", async () => {
    await page.goto(`${BASE_URL}/#/warn/rules`, { waitUntil: "domcontentloaded" });
    await page.waitForSelector(".ant-table-tbody tr", { timeout: 15000 });
    const rowCount = await page.locator(".ant-table-tbody tr").count();
    if (!rowCount) {
      return "No rule rows, skipped trigger";
    }
    const triggerBtn = page.locator(".ant-table-tbody tr").first().locator("button.ant-btn-link").nth(1);
    await triggerBtn.click();
    await page.waitForTimeout(1500);
    return "Clicked manual trigger on first rule";
  });

  await safeStep(report.ui.actions, "Message retry action", async () => {
    await page.goto(`${BASE_URL}/#/warn/messages`, { waitUntil: "domcontentloaded" });
    await page.waitForSelector(".ant-table-tbody tr", { timeout: 15000 });
    const enabledRetry = page.locator(".ant-table-tbody tr button.ant-btn-link:not([disabled])");
    const count = await enabledRetry.count();
    if (!count) {
      return "No retryable messages";
    }
    await enabledRetry.first().click();
    await page.waitForTimeout(1500);
    return "Clicked retry on one message";
  });

  await safeStep(report.ui.actions, "System dict refresh all cache", async () => {
    await page.goto(`${BASE_URL}/#/system/dicts`, { waitUntil: "domcontentloaded" });
    await page.waitForSelector(".table-toolbar-right .ant-btn", { timeout: 15000 });
    await page.locator(".table-toolbar-right .ant-btn").first().click();
    await page.waitForTimeout(1200);
    return "Triggered refresh all cache action";
  });

  await safeStep(report.ui.actions, "Open SOP task detail", async () => {
    await page.goto(`${BASE_URL}/#/sop/tasks`, { waitUntil: "domcontentloaded" });
    await page.waitForSelector(".ant-table-tbody tr", { timeout: 15000 });
    const detailBtn = page.locator(".ant-table-tbody tr button.ant-btn-link").first();
    const count = await detailBtn.count();
    if (!count) {
      return "No task rows";
    }
    await detailBtn.click();
    await page.waitForURL(/#\/sop\/tasks\/\d+/, { timeout: 15000 });
    await page.waitForSelector(".page-card", { timeout: 15000 });
    return "Opened task detail page";
  });

  const token = await page.evaluate(() => localStorage.getItem("mini_doamp_access_token") || "");
  if (!token) {
    pushResult(report.api.checks, "Acquire token from browser", false, "Token is empty");
  } else {
    pushResult(report.api.checks, "Acquire token from browser", true, "Token acquired");

    await safeStep(report.api.checks, "GET /warn/indexes", async () => {
      const r = await apiRequest(token, "GET", "/warn/indexes?pageNum=1&pageSize=10");
      if (r.status !== 200 || r.data?.code !== 200) throw new Error(`status=${r.status}, code=${r.data?.code}`);
      return `total=${r.data?.data?.total ?? "unknown"}`;
    });

    await safeStep(report.api.checks, "GET /warn/rules", async () => {
      const r = await apiRequest(token, "GET", "/warn/rules?pageNum=1&pageSize=10");
      if (r.status !== 200 || r.data?.code !== 200) throw new Error(`status=${r.status}, code=${r.data?.code}`);
      return `total=${r.data?.data?.total ?? "unknown"}`;
    });

    await safeStep(report.api.checks, "GET /warn/records", async () => {
      const r = await apiRequest(token, "GET", "/warn/records?pageNum=1&pageSize=10");
      if (r.status !== 200 || r.data?.code !== 200) throw new Error(`status=${r.status}, code=${r.data?.code}`);
      return `total=${r.data?.data?.total ?? "unknown"}`;
    });

    await safeStep(report.api.checks, "GET /msg/records", async () => {
      const r = await apiRequest(token, "GET", "/msg/records?pageNum=1&pageSize=10");
      if (r.status !== 200 || r.data?.code !== 200) throw new Error(`status=${r.status}, code=${r.data?.code}`);
      return `total=${r.data?.data?.total ?? "unknown"}`;
    });

    await safeStep(report.api.checks, "GET /sop/workflows", async () => {
      const r = await apiRequest(token, "GET", "/sop/workflows?pageNum=1&pageSize=10");
      if (r.status !== 200 || r.data?.code !== 200) throw new Error(`status=${r.status}, code=${r.data?.code}`);
      return `total=${r.data?.data?.total ?? "unknown"}`;
    });

    await safeStep(report.api.checks, "GET /sop/task-templates", async () => {
      const r = await apiRequest(token, "GET", "/sop/task-templates?pageNum=1&pageSize=10");
      if (r.status !== 200 || r.data?.code !== 200) throw new Error(`status=${r.status}, code=${r.data?.code}`);
      return `total=${r.data?.data?.total ?? "unknown"}`;
    });

    await safeStep(report.api.checks, "GET /sop/tasks", async () => {
      const r = await apiRequest(token, "GET", "/sop/tasks?pageNum=1&pageSize=10");
      if (r.status !== 200 || r.data?.code !== 200) throw new Error(`status=${r.status}, code=${r.data?.code}`);
      return `total=${r.data?.data?.total ?? "unknown"}`;
    });

    await safeStep(report.api.checks, "GET /sop/tasks/my-todo", async () => {
      const r = await apiRequest(token, "GET", "/sop/tasks/my-todo?pageNum=1&pageSize=10");
      if (r.status !== 200 || r.data?.code !== 200) throw new Error(`status=${r.status}, code=${r.data?.code}`);
      return `total=${r.data?.data?.total ?? "unknown"}`;
    });

    await safeStep(report.api.checks, "GET /sop/tasks/my-done", async () => {
      const r = await apiRequest(token, "GET", "/sop/tasks/my-done?pageNum=1&pageSize=10");
      if (r.status !== 200 || r.data?.code !== 200) throw new Error(`status=${r.status}, code=${r.data?.code}`);
      return `total=${r.data?.data?.total ?? "unknown"}`;
    });

    await safeStep(report.api.checks, "GET /dict", async () => {
      const r = await apiRequest(token, "GET", "/dict?pageNum=1&pageSize=10");
      if (r.status !== 200 || r.data?.code !== 200) throw new Error(`status=${r.status}, code=${r.data?.code}`);
      return `total=${r.data?.data?.total ?? "unknown"}`;
    });

    await safeStep(report.api.checks, "POST /cache/refresh/all", async () => {
      const r = await apiRequest(token, "POST", "/cache/refresh/all");
      if (r.status !== 200 || r.data?.code !== 200) throw new Error(`status=${r.status}, code=${r.data?.code}`);
      return `${r.data?.msg || "refresh all cache done"}`;
    });

    await safeStep(report.api.checks, "GET /system/job/list", async () => {
      const r = await apiRequest(token, "GET", "/system/job/list");
      if (r.status !== 200 || r.data?.code !== 200) throw new Error(`status=${r.status}, code=${r.data?.code}`);
      const len = Array.isArray(r.data?.data) ? r.data.data.length : 0;
      return `jobs=${len}`;
    });

    await safeStep(report.api.checks, "GET /system/job/log", async () => {
      const r = await apiRequest(token, "GET", "/system/job/log?pageNum=1&pageSize=10");
      if (r.status !== 200 || r.data?.code !== 200) throw new Error(`status=${r.status}, code=${r.data?.code}`);
      return `total=${r.data?.data?.total ?? "unknown"}`;
    });
  }

  await browser.close();

  report.endedAt = new Date().toISOString();
  const outDir = path.resolve(__dirname);
  const outPath = path.join(outDir, "full-e2e-report.json");
  fs.writeFileSync(outPath, JSON.stringify(report, null, 2), "utf-8");

  console.log(`Report written: ${outPath}`);
  console.log(`Passed: ${report.summary.passed}, Failed: ${report.summary.failed}`);

  if (report.summary.failed > 0) {
    process.exitCode = 1;
  }
}

run().catch(err => {
  console.error(err);
  process.exit(1);
});
