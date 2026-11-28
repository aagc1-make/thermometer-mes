[CmdletBinding()]
param(
    [string]$DifyDirectory = 'E:\dify\docker',
    [string]$BackendJar = (Join-Path $PSScriptRoot 'backend\ktg-mes\ktg-admin\target\ktg-admin.jar'),
    [switch]$RestartBackend
)

$ErrorActionPreference = 'Stop'

function Wait-Ready($Description, [int]$TimeoutSeconds, [scriptblock]$Check) {
    $timer = [Diagnostics.Stopwatch]::StartNew()
    do {
        if (& $Check) { return }
        Start-Sleep -Seconds 2
    } while ($timer.Elapsed.TotalSeconds -lt $TimeoutSeconds)
    throw "Timed out waiting for $Description."
}

function Test-MesApi($Url) {
    try { (Invoke-RestMethod -Uri $Url -TimeoutSec 4).code -eq 200 }
    catch { $false }
}

$BackendJar = (Resolve-Path -LiteralPath $BackendJar).Path
$DifyDirectory = (Resolve-Path -LiteralPath $DifyDirectory).Path
$docker = (Get-Command docker.exe).Source
$keyFile = Join-Path $PSScriptRoot 'ai-keys.txt'
if (-not (Test-Path -LiteralPath $keyFile)) { throw 'Dify requires the local ai-keys.txt file.' }
$dify = ConvertFrom-StringData -StringData ((Get-Content -LiteralPath $keyFile |
    Where-Object { $_ -match '^MES_AI_DIFY_(URL|KEY|CONSOLE_WORKFLOW_URL)=' }) -join [Environment]::NewLine)
if ($dify.MES_AI_DIFY_URL -notmatch '^http://(?:localhost|127\.0\.0\.1):\d+/v1/?$' -or -not $dify.MES_AI_DIFY_KEY) {
    throw 'Set the local MES_AI_DIFY_URL and MES_AI_DIFY_KEY in ai-keys.txt.'
}

function Test-DockerEngine {
    $null = & $docker info --format '{{.ServerVersion}}' 2>$null
    $LASTEXITCODE -eq 0
}

if (-not (Test-DockerEngine)) {
    $desktop = Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'
    if (-not (Get-Process 'Docker Desktop' -ErrorAction SilentlyContinue)) {
        Start-Process -FilePath $desktop -WindowStyle Hidden
    }
    Wait-Ready 'Docker Desktop' 90 { Test-DockerEngine }
}

Write-Host 'Starting Dify Compose services...'
Push-Location -LiteralPath $DifyDirectory
try {
    & $docker compose up -d --no-recreate --wait --wait-timeout 120
    if ($LASTEXITCODE -ne 0) { throw 'Dify Compose failed to start.' }
} finally { Pop-Location }

$difyInfo = $dify.MES_AI_DIFY_URL.TrimEnd('/') + '/info'
try {
    $null = Invoke-WebRequest -UseBasicParsing -Uri $difyInfo -TimeoutSec 10 -Headers @{
        Authorization = 'Bearer ' + $dify.MES_AI_DIFY_KEY
    }
} catch { throw 'Dify is running, but its application API could not be verified. Check the server and key in ai-keys.txt.' }

$backUrl = 'http://127.0.0.1:8080/captchaImage'
$frontUrl = 'http://localhost:8095'
$logDirectory = Join-Path $PSScriptRoot 'runtime\mes-start'
New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null

function Start-Launcher($Name, $File, [hashtable]$Settings) {
    $previous = @{}
    try {
        foreach ($key in $Settings.Keys) {
            $previous[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
            [Environment]::SetEnvironmentVariable($key, $Settings[$key], 'Process')
        }
        $batch = Join-Path $PSScriptRoot $File
        Start-Process -FilePath $env:ComSpec -ArgumentList @('/d', '/c', ('""{0}""' -f $batch)) `
            -WorkingDirectory $PSScriptRoot -WindowStyle Hidden `
            -RedirectStandardOutput (Join-Path $logDirectory "$Name.log") `
            -RedirectStandardError (Join-Path $logDirectory "$Name.err.log")
    } finally {
        foreach ($key in $previous.Keys) {
            [Environment]::SetEnvironmentVariable($key, $previous[$key], 'Process')
        }
    }
}

# Windows PowerShell 5.1 Start-Process throws when both upper- and lower-case
# proxy variables exist. The local MES backend and its dev proxy do not need them.
foreach ($proxyName in @('HTTP_PROXY', 'HTTPS_PROXY', 'NO_PROXY', 'http_proxy', 'https_proxy', 'no_proxy')) {
    [Environment]::SetEnvironmentVariable($proxyName, $null, 'Process')
}

$backendListeners = @(Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue)
if ($RestartBackend -and $backendListeners.Count) {
    $owners = @($backendListeners.OwningProcess | Sort-Object -Unique)
    if ($owners.Count -ne 1) { throw 'Port 8080 has multiple owners. Restart MES manually.' }
    $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $($owners[0])"
    if ($processInfo.Name -ne 'java.exe' -or -not $processInfo.CommandLine.Contains($BackendJar)) {
        throw 'Port 8080 belongs to a different process or JAR. Pass its MES JAR using -BackendJar, or stop it manually.'
    }
    Stop-Process -Id $owners[0]
    Wait-Ready 'the old MES backend to release port 8080' 15 {
        -not (Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue)
    }
    $backendListeners = @()
}

if (-not $backendListeners.Count) {
    $springDifySettings = @{
        'mes.ai.dify.api-base-url' = $dify.MES_AI_DIFY_URL
        'mes.ai.dify.api-key' = $dify.MES_AI_DIFY_KEY
    }
    if ($dify.MES_AI_DIFY_CONSOLE_WORKFLOW_URL) {
        $springDifySettings['mes.ai.dify.console-workflow-url'] = $dify.MES_AI_DIFY_CONSOLE_WORKFLOW_URL
    }
    $springDify = $springDifySettings | ConvertTo-Json -Compress
    Start-Launcher 'backend' 'start-backend.bat' @{
        MES_BACKEND_JAR = $BackendJar
        SERVER_ADDRESS = '127.0.0.1'
        SPRING_CONFIG_ADDITIONAL_LOCATION = 'optional:file:./ktg-admin/src/main/resources/'
        SPRING_APPLICATION_JSON = $springDify
    }
} else {
    Write-Host 'The existing MES backend was kept. Use -RestartBackend to reload Dify settings.'
}
Wait-Ready "MES backend (see $logDirectory\backend.log)" 120 { Test-MesApi $backUrl }

& $docker compose --project-directory $DifyDirectory exec -T api curl -fsS --max-time 8 -o /dev/null `
    'http://host.docker.internal:8080/mes/ai/status'
if ($LASTEXITCODE -ne 0) { throw 'The Dify API container cannot reach MES on host.docker.internal:8080.' }

if (-not (Get-NetTCPConnection -State Listen -LocalPort 8095 -ErrorAction SilentlyContinue)) {
    $frontendSettings = @{ MES_BACKEND_URL = 'http://127.0.0.1:8080' }
    $nodeVersion = & node.exe --version
    if ([int](($nodeVersion.TrimStart('v') -split '\.')[0]) -ge 17) {
        $frontendSettings.NODE_OPTIONS = "$env:NODE_OPTIONS --openssl-legacy-provider".Trim()
    }
    Start-Launcher 'frontend' 'start-frontend.bat' $frontendSettings
}
Wait-Ready "MES frontend proxy (see $logDirectory\frontend.log)" 120 { Test-MesApi "$frontUrl/dev-api/captchaImage" }

Write-Host "MES: $frontUrl/login"
Write-Host 'Dify API verified. The MES badge becomes Online after the first successful workflow receipt.'
