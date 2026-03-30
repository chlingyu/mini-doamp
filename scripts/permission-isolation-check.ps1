param(
  [string]$BaseUrl = "http://localhost:8090/api",
  [string]$AdminUser = "admin",
  [string]$AdminPass = "admin123",
  [string]$OperatorUser = "operator",
  [string]$OperatorPass = "admin123",
  [string]$OutFile = ".codex-run/permission-isolation-report.json"
)

$ErrorActionPreference = "Stop"

function Invoke-ApiRaw {
  param(
    [string]$Method,
    [string]$Url,
    [string]$Token = "",
    $Body = $null
  )

  $headers = @{}
  if ($Token) {
    $headers.Authorization = "Bearer $Token"
  }

  try {
    if ($null -ne $Body) {
      $resp = Invoke-RestMethod -Method $Method -Uri $Url -Headers $headers -ContentType "application/json" -Body ($Body | ConvertTo-Json -Depth 20)
    } else {
      $resp = Invoke-RestMethod -Method $Method -Uri $Url -Headers $headers
    }
    return [pscustomobject]@{
      ok = $true
      status = 200
      code = if ($null -ne $resp.code) { [int]$resp.code } else { 0 }
      msg = if ($null -ne $resp.msg) { [string]$resp.msg } else { "" }
      data = $resp.data
      raw = $resp
    }
  } catch {
    $status = 0
    $message = $_.Exception.Message
    if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
      $status = [int]$_.Exception.Response.StatusCode
    }
    return [pscustomobject]@{
      ok = $false
      status = $status
      code = $status
      msg = $message
      data = $null
      raw = $null
    }
  }
}

function Login {
  param(
    [string]$Username,
    [string]$Password
  )
  $r = Invoke-ApiRaw -Method "POST" -Url "$BaseUrl/auth/login" -Body @{ username = $Username; password = $Password }
  if (-not $r.ok -or $r.code -ne 200 -or -not $r.raw.data.token) {
    throw "Login failed for user=$Username, status=$($r.status), code=$($r.code), msg=$($r.msg)"
  }
  return $r.raw.data.token
}

function Expect-Status {
  param(
    [string]$Name,
    [string]$Method,
    [string]$Path,
    [string]$Token,
    [int]$ExpectedStatus,
    $Body = $null
  )
  $r = Invoke-ApiRaw -Method $Method -Url "$BaseUrl$Path" -Token $Token -Body $Body
  return [pscustomobject]@{
    name = $Name
    method = $Method
    path = $Path
    expectedStatus = $ExpectedStatus
    actualStatus = $r.status
    pass = ($r.status -eq $ExpectedStatus)
    responseCodeField = $r.code
    responseMsg = $r.msg
  }
}

$adminToken = Login -Username $AdminUser -Password $AdminPass
$operatorToken = Login -Username $OperatorUser -Password $OperatorPass

$adminInfo = Invoke-ApiRaw -Method "GET" -Url "$BaseUrl/auth/userInfo" -Token $adminToken
$operatorInfo = Invoke-ApiRaw -Method "GET" -Url "$BaseUrl/auth/userInfo" -Token $operatorToken

$checks = @()

# 1) system.user protected endpoint: admin 200, operator 403
$checks += Expect-Status -Name "Admin can GET /users" -Method "GET" -Path "/users?pageNum=1&pageSize=5" -Token $adminToken -ExpectedStatus 200
$checks += Expect-Status -Name "Operator forbidden GET /users" -Method "GET" -Path "/users?pageNum=1&pageSize=5" -Token $operatorToken -ExpectedStatus 403

# 2) system.job protected endpoint: both allowed in current seed permissions
$checks += Expect-Status -Name "Admin can GET /system/job/list" -Method "GET" -Path "/system/job/list" -Token $adminToken -ExpectedStatus 200
$checks += Expect-Status -Name "Operator can GET /system/job/list" -Method "GET" -Path "/system/job/list" -Token $operatorToken -ExpectedStatus 200

# 3) warn.rule protected endpoint: both should be allowed for operator role in seed data
$checks += Expect-Status -Name "Admin can GET /warn/rules" -Method "GET" -Path "/warn/rules?pageNum=1&pageSize=5" -Token $adminToken -ExpectedStatus 200
$checks += Expect-Status -Name "Operator can GET /warn/rules" -Method "GET" -Path "/warn/rules?pageNum=1&pageSize=5" -Token $operatorToken -ExpectedStatus 200

# 4) sop.task protected endpoint: both should be allowed
$checks += Expect-Status -Name "Admin can GET /sop/tasks" -Method "GET" -Path "/sop/tasks?pageNum=1&pageSize=5" -Token $adminToken -ExpectedStatus 200
$checks += Expect-Status -Name "Operator can GET /sop/tasks" -Method "GET" -Path "/sop/tasks?pageNum=1&pageSize=5" -Token $operatorToken -ExpectedStatus 200

# 5) endpoints operator should be forbidden by backend
$checks += Expect-Status -Name "Operator forbidden GET /sop/workflows" -Method "GET" -Path "/sop/workflows?pageNum=1&pageSize=5" -Token $operatorToken -ExpectedStatus 403
$checks += Expect-Status -Name "Operator forbidden GET /sop/task-templates" -Method "GET" -Path "/sop/task-templates?pageNum=1&pageSize=5" -Token $operatorToken -ExpectedStatus 403

# 6) unauthenticated request should be 401 for protected API
$checks += Expect-Status -Name "Anonymous blocked GET /warn/rules" -Method "GET" -Path "/warn/rules?pageNum=1&pageSize=5" -Token "" -ExpectedStatus 401

$passCount = @($checks | Where-Object { $_.pass }).Count
$failures = @($checks | Where-Object { -not $_.pass })

$report = [ordered]@{
  timestamp = (Get-Date).ToString("s")
  baseUrl = $BaseUrl
  principals = [ordered]@{
    admin = [ordered]@{
      username = $AdminUser
      permissions = @($adminInfo.raw.data.permissions)
    }
    operator = [ordered]@{
      username = $OperatorUser
      permissions = @($operatorInfo.raw.data.permissions)
    }
  }
  summary = [ordered]@{
    total = $checks.Count
    passed = $passCount
    failed = $failures.Count
  }
  checks = $checks
  failures = $failures
}

New-Item -ItemType Directory -Force (Split-Path -Parent $OutFile) | Out-Null
$report | ConvertTo-Json -Depth 10 | Set-Content -Path $OutFile -Encoding UTF8

Write-Host "Permission report written to: $OutFile"
Write-Host ($report.summary | ConvertTo-Json -Depth 5)

if ($failures.Count -gt 0) {
  exit 1
}
