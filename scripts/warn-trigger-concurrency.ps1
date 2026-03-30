param(
  [string]$BaseUrl = "http://localhost:8090/api",
  [string]$Username = "admin",
  [string]$Password = "admin123",
  [int]$Concurrency = 20,
  [int]$RuleId = 0,
  [string]$OutFile = ".codex-run/warn-trigger-concurrency-report.json"
)

$ErrorActionPreference = "Stop"

function Invoke-Api {
  param(
    [string]$Method,
    [string]$Url,
    [hashtable]$Headers = @{},
    $Body = $null
  )

  if ($null -ne $Body) {
    return Invoke-RestMethod -Method $Method -Uri $Url -Headers $Headers -ContentType "application/json" -Body ($Body | ConvertTo-Json -Depth 20)
  }

  return Invoke-RestMethod -Method $Method -Uri $Url -Headers $Headers
}

Write-Host "== Login =="
$login = Invoke-Api -Method "POST" -Url "$BaseUrl/auth/login" -Body @{ username = $Username; password = $Password }
if ($login.code -ne 200 -or -not $login.data.token) {
  throw "Login failed"
}
$token = $login.data.token
$headers = @{ Authorization = "Bearer $token" }

if ($RuleId -le 0) {
  Write-Host "== Resolve RuleId =="
  $rules = Invoke-Api -Method "GET" -Url "$BaseUrl/warn/rules?pageNum=1&pageSize=1" -Headers $headers
  if ($rules.code -ne 200 -or -not $rules.data.records -or $rules.data.records.Count -eq 0) {
    throw "No warn rules found. Please create at least one rule first."
  }
  $RuleId = [int]$rules.data.records[0].id
}

Write-Host "Using RuleId: $RuleId"

$beforeRecords = Invoke-Api -Method "GET" -Url "$BaseUrl/warn/records?pageNum=1&pageSize=1" -Headers $headers
$beforeTotal = [int]$beforeRecords.data.total

Write-Host "== Run $Concurrency concurrent trigger requests =="
$jobs = @()
$suiteWatch = [System.Diagnostics.Stopwatch]::StartNew()

for ($i = 1; $i -le $Concurrency; $i++) {
  $jobs += Start-Job -ScriptBlock {
    param($InnerBaseUrl, $InnerToken, $InnerRuleId, $Index)

    $h = @{ Authorization = "Bearer $InnerToken" }
    $w = [System.Diagnostics.Stopwatch]::StartNew()
    try {
      $resp = Invoke-RestMethod -Method "POST" -Uri "$InnerBaseUrl/warn/rules/$InnerRuleId/trigger" -Headers $h
      $w.Stop()
      [pscustomobject]@{
        index = $Index
        ok = ($resp.code -eq 200)
        httpCode = if ($resp.code) { [int]$resp.code } else { 0 }
        durationMs = [math]::Round($w.Elapsed.TotalMilliseconds, 2)
        triggeredCount = if ($null -ne $resp.data) { [int]$resp.data } else { 0 }
        error = $null
      }
    } catch {
      $w.Stop()
      [pscustomobject]@{
        index = $Index
        ok = $false
        httpCode = 0
        durationMs = [math]::Round($w.Elapsed.TotalMilliseconds, 2)
        triggeredCount = 0
        error = $_.Exception.Message
      }
    }
  } -ArgumentList $BaseUrl, $token, $RuleId, $i
}

Wait-Job -Job $jobs | Out-Null
$results = Receive-Job -Job $jobs
$jobs | Remove-Job -Force | Out-Null
$suiteWatch.Stop()

$afterRecords = Invoke-Api -Method "GET" -Url "$BaseUrl/warn/records?pageNum=1&pageSize=1" -Headers $headers
$afterTotal = [int]$afterRecords.data.total

$durations = @($results | ForEach-Object { [double]$_.durationMs } | Sort-Object)
$success = @($results | Where-Object { $_.ok })
$failed = @($results | Where-Object { -not $_.ok })
$sumTriggered = ($results | Measure-Object -Property triggeredCount -Sum).Sum
$actualRecordDelta = $afterTotal - $beforeTotal
$p95Index = [math]::Min([math]::Ceiling($durations.Count * 0.95) - 1, $durations.Count - 1)

$summary = [ordered]@{
  timestamp = (Get-Date).ToString("s")
  baseUrl = $BaseUrl
  ruleId = $RuleId
  concurrency = $Concurrency
  suiteDurationMs = [math]::Round($suiteWatch.Elapsed.TotalMilliseconds, 2)
  successCount = $success.Count
  failedCount = $failed.Count
  minMs = if ($durations.Count -gt 0) { $durations[0] } else { 0 }
  avgMs = if ($durations.Count -gt 0) { [math]::Round(($durations | Measure-Object -Average).Average, 2) } else { 0 }
  p95Ms = if ($durations.Count -gt 0) { $durations[$p95Index] } else { 0 }
  maxMs = if ($durations.Count -gt 0) { $durations[$durations.Count - 1] } else { 0 }
  beforeWarnRecordTotal = $beforeTotal
  afterWarnRecordTotal = $afterTotal
  actualWarnRecordDelta = $actualRecordDelta
  sumTriggeredByApi = [int]$sumTriggered
  recordDeltaMatchesApiSum = ($actualRecordDelta -eq [int]$sumTriggered)
}

$report = [ordered]@{
  summary = $summary
  failures = @($failed)
  results = @($results | Sort-Object index)
}

New-Item -ItemType Directory -Force (Split-Path -Parent $OutFile) | Out-Null
$report | ConvertTo-Json -Depth 10 | Set-Content -Path $OutFile -Encoding UTF8

Write-Host "== Summary =="
$summary | ConvertTo-Json -Depth 5
Write-Host "Report written to: $OutFile"
