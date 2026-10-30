@echo off
setlocal
title Stop MES + MiniApp all services

rem ============================================================
rem  Shutdown counterpart of start-all.bat
rem  Stops: MES backend 8080 / MES frontend 80(81) /
rem         MiniApp API 8081 / MiniApp H5 10086 / Redis
rem  MySQL is NOT managed and stays running.
rem ============================================================

echo ============================================
echo  [1/3] Close service windows (incl. child java/node)
echo ============================================
for %%T in ("MES-Backend" "MES-Frontend" "MiniApp-API" "MiniApp-H5" "MES-Redis") do (
    taskkill /FI "WINDOWTITLE eq %%~T*" /T /F >nul 2>&1
    if not errorlevel 1 (echo     closed window %%~T) else (echo     window %%~T not found - skip)
)

echo ============================================
echo  [2/3] Kill leftover processes by port
echo ============================================
for %%P in (8080 8081 10086 80 81) do (
    call :kill_port %%P
)

echo ============================================
echo  [3/3] Redis
echo ============================================
taskkill /IM redis-server.exe /F >nul 2>&1
if not errorlevel 1 (echo     redis-server stopped) else (echo     redis-server not running - skip)

echo.
echo ============================================
echo  Port status check:
echo ============================================
for %%P in (8080 8081 10086 6379 80 81) do (
    call :port_open %%P
    if not errorlevel 1 (echo     [WARN] port %%P still listening) else (echo     port %%P closed)
)

echo.
echo All managed services stopped. MySQL left running.
pause
exit /b 0

rem ---------- helpers ----------
:kill_port
for /f "tokens=5" %%a in ('netstat -ano ^| findstr /R /C:":%1 .*LISTENING"') do (
    echo     port %1 occupied by PID %%a - killing ...
    taskkill /PID %%a /F >nul 2>&1
)
exit /b

:port_open
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -Command "try { $c = New-Object Net.Sockets.TcpClient; $c.Connect('127.0.0.1', %1); $c.Close(); exit 0 } catch { exit 1 }"
exit /b
