[CmdletBinding()]
param(
    [string]$StackRoot = 'E:\dev\testlink-stack',
    [string]$MySQLHome = 'E:\D\mysql-8.0.44-winx64'
)
$ErrorActionPreference = 'Stop'
$stack = [IO.Path]::GetFullPath($StackRoot).TrimEnd('\')
function Get-StackOwner([int]$Port, [string]$Image, [string]$Marker) {
    $listener = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $listener) { return $null }
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
    $command = ($process.CommandLine -replace '\\','/').ToLowerInvariant()
    if ($process.Name -ne $Image -or -not $command.Contains(($Marker -replace '\\','/').ToLowerInvariant())) {
        throw "Port $Port belongs to another process. Nothing was stopped."
    }
    return $process.ProcessId
}
# Validate both owners before stopping either one.
$phpPid = Get-StackOwner 8085 'php.exe' (Join-Path $stack 'testlink')
$mysqlPid = Get-StackOwner 3309 'mysqld.exe' (Join-Path $stack 'testlink-mysqldata')
if ($phpPid) { Stop-Process -Id $phpPid -ErrorAction Stop }
if ($mysqlPid) {
    & (Join-Path $MySQLHome 'bin\mysqladmin.exe') --no-defaults '--host=127.0.0.1' --port=3309 --user=root shutdown
    if ($LASTEXITCODE -ne 0) { throw 'MySQL graceful shutdown failed; no forced termination was attempted.' }
}
Write-Output 'Stopped the TestLink PHP server and dedicated MySQL on 3309.'
