[CmdletBinding()]
param(
    [string]$StackRoot = 'E:\dev\testlink-stack',
    [string]$MySQLHome = 'E:\D\mysql-8.0.44-winx64'
)
$ErrorActionPreference = 'Stop'
$stack = [IO.Path]::GetFullPath($StackRoot).TrimEnd('\')
$data = Join-Path $stack 'testlink-mysqldata'
$php = Join-Path $stack 'php74\php.exe'
$web = Join-Path $stack 'testlink'
$mysql = Join-Path $MySQLHome 'bin\mysqld.exe'
foreach ($required in @($php, $mysql, (Join-Path $web 'config_db.inc.php'), (Join-Path $data 'auto.cnf'))) {
    if (-not (Test-Path -LiteralPath $required)) { throw "Installed component missing: $required" }
}
function Get-StackListener([int]$Port, [string]$Image, [string]$Marker) {
    $listener = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $listener) { return $null }
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
    $command = ($process.CommandLine -replace '\\','/').ToLowerInvariant()
    if ($process.Name -ne $Image -or -not $command.Contains(($Marker -replace '\\','/').ToLowerInvariant())) {
        throw "Port $Port belongs to another process. No process was changed."
    }
    return $process.ProcessId
}
function Wait-LocalPort([int]$Port) {
    for ($attempt = 0; $attempt -lt 40; $attempt++) {
        $client = [Net.Sockets.TcpClient]::new()
        try { $client.Connect('127.0.0.1', $Port); return } catch { Start-Sleep -Milliseconds 250 } finally { $client.Dispose() }
    }
    throw "Port $Port did not become ready. Check logs in $stack"
}
if (-not (Get-StackListener 3309 'mysqld.exe' $data)) {
    $mysqlArguments = @('--no-defaults', ('--basedir="' + ($MySQLHome -replace '\\','/') + '"'),
        ('--datadir="' + ($data -replace '\\','/') + '"'), '--port=3309', '--bind-address=127.0.0.1',
        '--mysqlx=OFF', '--sql-mode=NO_ENGINE_SUBSTITUTION', '--console')
    Start-Process -FilePath $mysql -ArgumentList $mysqlArguments -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $stack 'testlink-mysql.stdout.log') `
        -RedirectStandardError (Join-Path $stack 'testlink-mysql.stderr.log') | Out-Null
    Wait-LocalPort 3309
}
if (-not (Get-StackListener 8085 'php.exe' $web)) {
    Start-Process -FilePath $php -ArgumentList @('-S','127.0.0.1:8085','-t',('"'+$web+'"')) `
        -WorkingDirectory $web -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $stack 'php-web.stdout.log') `
        -RedirectStandardError (Join-Path $stack 'php-web.stderr.log') | Out-Null
    Wait-LocalPort 8085
}
$response = Invoke-WebRequest -Uri 'http://127.0.0.1:8085/login.php' -UseBasicParsing
if ($response.StatusCode -ne 200) { throw 'TestLink login health check failed' }
Write-Output 'TestLink ready: http://127.0.0.1:8085'
Write-Output ('Local accounts: ' + (Join-Path $stack 'credentials.local.json'))
