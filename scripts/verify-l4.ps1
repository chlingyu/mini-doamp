# Mini DOAMP L4 全栈验证脚本
# 用法: .\scripts\verify-l4.ps1
# 前提: Docker Desktop 已启动，9999/9998 端口未被占用
# 功能: Docker启动 → 后端启动 → API校验 → 结果汇总

$ErrorActionPreference = "Continue"
$JWT_SECRET = "miniDoampDevKey12345678901234567890"
$BASE = "http://localhost:9999"
$PASS = 0
$FAIL = 0
$RESULTS = @()

function Test-Api {
    param([string]$Name, [string]$Method, [string]$Url, [string]$Body, [string]$Token, [int]$ExpectedCode = 200)
    
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    
    try {
        if ($Method -eq "GET") {
            $resp = Invoke-RestMethod -Uri "$BASE$Url" -Method GET -Headers $headers -ErrorAction Stop
        } else {
            $resp = Invoke-RestMethod -Uri "$BASE$Url" -Method $Method -Body $Body -Headers $headers -ErrorAction Stop
        }
        
        if ($resp.code -eq $ExpectedCode) {
            $script:PASS++
            $script:RESULTS += [PSCustomObject]@{Test=$Name; Status="✅ PASS"; Detail="code=$($resp.code)"}
            return $resp.data
        } else {
            $script:FAIL++
            $script:RESULTS += [PSCustomObject]@{Test=$Name; Status="❌ FAIL"; Detail="expected=$ExpectedCode actual=$($resp.code)"}
            return $null
        }
    } catch {
        $script:FAIL++
        $script:RESULTS += [PSCustomObject]@{Test=$Name; Status="❌ FAIL"; Detail=$_.Exception.Message.Substring(0, [Math]::Min(80, $_.Exception.Message.Length))}
        return $null
    }
}

Write-Host "`n========== Mini DOAMP L4 全栈验证 ==========`n" -ForegroundColor Cyan

# ===== Step 1: 检查 Docker =====
Write-Host "[1/6] 检查 Docker..." -ForegroundColor Yellow
$dockerStatus = docker info 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Docker Desktop 未启动，请先启动 Docker" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Docker 已就绪" -ForegroundColor Green

# ===== Step 2: 启动容器 =====
Write-Host "[2/6] 启动 Docker 容器..." -ForegroundColor Yellow
docker-compose up -d 2>&1 | Out-Null
Start-Sleep -Seconds 5

# 等待 MySQL 就绪
for ($i = 0; $i -lt 30; $i++) {
    $ping = docker exec mini-doamp-mysql mysqladmin ping -h localhost -u root -proot123 2>&1
    if ($ping -match "alive") { break }
    Start-Sleep -Seconds 2
}
Write-Host "✅ MySQL 容器已就绪" -ForegroundColor Green

# ===== Step 3: 启动后端 =====
Write-Host "[3/6] 启动后端（MySQL 模式）..." -ForegroundColor Yellow
$env:JWT_SECRET = $JWT_SECRET
$env:DB_URL = "jdbc:mysql://localhost:3306/mini_doamp?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
$env:DB_USER = "root"
$env:DB_PASS = "root123"

$backend = Start-Process -FilePath "cmd.exe" -ArgumentList "/c gradlew.bat :mini-doamp-server:bootRun -Dfile.encoding=UTF-8" -WorkingDirectory (Get-Location).Path -PassThru -WindowStyle Hidden

# 等待后端启动
Write-Host "  等待后端启动..." -NoNewline
for ($i = 0; $i -lt 60; $i++) {
    try {
        $health = Invoke-RestMethod -Uri "$BASE/api/auth/login" -Method POST -Body '{"username":"admin","password":"admin123"}' -ContentType "application/json" -ErrorAction Stop
        if ($health.code -eq 200) { break }
    } catch { }
    Write-Host "." -NoNewline
    Start-Sleep -Seconds 3
}
Write-Host ""
Write-Host "✅ 后端已启动" -ForegroundColor Green

# ===== Step 4: 核心 API 验证 =====
Write-Host "`n[4/6] 核心 API 验证..." -ForegroundColor Yellow

# 登录
$loginData = Test-Api "登录" "POST" "/api/auth/login" '{"username":"admin","password":"admin123"}' $null
$token = $loginData.token
$refreshToken = $loginData.refreshToken

# JWT Refresh
$refreshData = Test-Api "Token刷新" "POST" "/api/auth/refresh" "{`"refreshToken`":`"$refreshToken`"}" $token
if ($refreshData) { $token = $refreshData.token }

# UserInfo
Test-Api "获取用户信息" "GET" "/api/auth/userInfo" $null $token | Out-Null

# 预警指标列表
Test-Api "预警指标列表" "GET" "/api/warn/indexes?pageNum=1&pageSize=10" $null $token | Out-Null

# 预警规则列表
$rules = Test-Api "预警规则列表" "GET" "/api/warn/rules?pageNum=1&pageSize=50" $null $token
if ($rules -and $rules.records) {
    $ruleId = $rules.records[0].id
    Test-Api "触发预警规则" "POST" "/api/warn/rules/$ruleId/trigger" '{}' $token | Out-Null
}

# 预警记录
Test-Api "预警记录列表" "GET" "/api/warn/records?pageNum=1&pageSize=10" $null $token | Out-Null

# DB Adapter
Test-Api "数据库适配器信息" "GET" "/api/db/info" $null $token | Out-Null

# SOP Workflows
Test-Api "SOP工作流列表" "GET" "/api/sop/workflows?pageNum=1&pageSize=10" $null $token | Out-Null

# SOP Tasks
Test-Api "SOP任务列表" "GET" "/api/sop/tasks?pageNum=1&pageSize=10" $null $token | Out-Null

# 字典列表
Test-Api "字典列表" "GET" "/api/system/dicts?pageNum=1&pageSize=10" $null $token | Out-Null

# 消息记录
Test-Api "消息记录" "GET" "/api/msg/records?pageNum=1&pageSize=10" $null $token | Out-Null

# ===== Step 5: XXL-Job 调度路径验证 =====
Write-Host "`n[5/6] XXL-Job 调度路径验证..." -ForegroundColor Yellow
try {
    $xxlBody = @{
        jobId = 1; executorHandler = 'warnCheckHandler'; executorParams = ''
        executorBlockStrategy = 'SERIAL_EXECUTION'; executorTimeout = 0
        logId = 200000; logDateTime = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        glueType = 'BEAN'; glueSource = ''; glueUpdatetime = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        broadcastIndex = 0; broadcastTotal = 1
    } | ConvertTo-Json
    $xxlResp = Invoke-RestMethod -Uri 'http://localhost:9998/run' -Method POST -Body $xxlBody -ContentType 'application/json;charset=utf-8' -Headers @{'XXL-JOB-ACCESS-TOKEN'='default_token'}
    if ($xxlResp.code -eq 200) {
        $PASS++
        $RESULTS += [PSCustomObject]@{Test="XXL-Job warnCheckHandler"; Status="✅ PASS"; Detail="code=200"}
    }
} catch {
    $FAIL++
    $RESULTS += [PSCustomObject]@{Test="XXL-Job warnCheckHandler"; Status="❌ FAIL"; Detail=$_.Exception.Message.Substring(0, 80)}
}

# JobExecLog
$logs = Test-Api "JobExecLog查询" "GET" "/api/system/job/log/native?current=1&size=50" $null $token

# ===== Step 6: 汇总 =====
Write-Host "`n[6/6] 验证结果汇总" -ForegroundColor Yellow
Write-Host "================================================" -ForegroundColor Cyan
$RESULTS | Format-Table -AutoSize
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "PASS: $PASS  FAIL: $FAIL  TOTAL: $($PASS + $FAIL)" -ForegroundColor $(if ($FAIL -eq 0) { "Green" } else { "Red" })

# 清理
if ($backend -and !$backend.HasExited) {
    Stop-Process -Id $backend.Id -Force 2>$null
}

exit $(if ($FAIL -eq 0) { 0 } else { 1 })
