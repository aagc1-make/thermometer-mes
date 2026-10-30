@echo off
setlocal EnableDelayedExpansion
title MES + MiniApp One-Click Start

rem ============================================================
rem  Thermometer MES + MiniApp one-click startup
rem    [1] MySQL          :3306  (check only)
rem    [2] Redis          :6379  (auto start, no password)
rem    [3] MES backend    :8080  backend\ktg-mes\ktg-admin\target\ktg-admin.jar
rem    [4] MES frontend   :80    ktg-mes-ui (npm run dev, falls back to 81)
rem    [5] MiniApp API    :8081  backend\miniapp-api\target\miniapp-api.jar
rem    [6] MiniApp H5     :10086 miniapp (npm run dev:h5)
rem  NOTE: KTG_DB_PASSWORD fallback below is the local DB password.
rem        DO NOT commit this file to git.
rem ============================================================

set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
set "REDIS_HOME=%ROOT%\tools\redis"
if not defined KTG_DB_PASSWORD set "KTG_DB_PASSWORD=yfq13619423346"
rem MES 智能助手：只透传系统里已存在的环境变量，不在脚本里存密钥
set "AI_KEY_ARG="
if defined MES_AI_KEY set "AI_KEY_ARG=--mes.ai.deepseek-key=%MES_AI_KEY%"

echo ============================================
echo  [1/6] MySQL :3306
echo ============================================
call :port_open 3306
if errorlevel 1 (
    echo     [WARN] port 3306 not listening. Start local MySQL first.
) else (
    echo     already listening - skip
)

echo ============================================
echo  [2/6] Redis :6379
echo ============================================
call :port_open 6379
if errorlevel 1 (
    echo     not running - starting redis-server ...
    start "MES-Redis" /min /D "%REDIS_HOME%" redis-server.exe redis.windows.conf
    call :wait_port 6379 15
) else (
    echo     already listening - skip
)

echo ============================================
echo  [3/6] MES Backend :8080
echo ============================================
call :port_open 8080
if not errorlevel 1 (
    echo     already listening - skip
) else (
    if not exist "%ROOT%\backend\ktg-mes\ktg-admin\target\ktg-admin.jar" (
        echo     jar not found - building with maven ...
        pushd "%ROOT%\backend\ktg-mes"
        call mvn -q -DskipTests package "-Dlombok.version=1.18.34"
        popd
    )
    if not exist "%ROOT%\backend\ktg-mes\upload" mkdir "%ROOT%\backend\ktg-mes\upload"
    start "MES-Backend" /D "%ROOT%\backend\ktg-mes" cmd /k "java -Dfile.encoding=UTF-8 -jar ktg-admin\target\ktg-admin.jar %AI_KEY_ARG%"
    echo     started in new window MES-Backend
)

echo ============================================
echo  [4/6] MES Frontend :80
echo ============================================
call :port_open 80
if not errorlevel 1 (
    echo     already listening - skip
) else (
    if not exist "%ROOT%\ktg-mes-ui\node_modules\.bin\vue-cli-service.cmd" (
        echo     vue-cli-service missing - reinstalling dependencies ...
        pushd "%ROOT%\ktg-mes-ui"
        call npm install --legacy-peer-deps
        popd
    )
    start "MES-Frontend" /D "%ROOT%\ktg-mes-ui" cmd /k "set NODE_OPTIONS=--openssl-legacy-provider && npm run dev"
    echo     started in new window MES-Frontend
)

echo ============================================
echo  [5/6] MiniApp API :8081
echo ============================================
call :port_open 8081
if not errorlevel 1 (
    echo     already listening - skip
) else (
    if not exist "%ROOT%\backend\miniapp-api\target\miniapp-api.jar" (
        echo     jar not found - building with maven ...
        pushd "%ROOT%\backend\miniapp-api"
        call mvn -q -DskipTests package
        popd
    )
    start "MiniApp-API" /D "%ROOT%\backend\miniapp-api" cmd /k "java -Dfile.encoding=UTF-8 -jar target\miniapp-api.jar"
    echo     started in new window MiniApp-API
)

echo ============================================
echo  [6/6] MiniApp H5 :10086
echo ============================================
call :port_open 10086
if not errorlevel 1 (
    echo     already listening - skip
) else (
    if not exist "%ROOT%\miniapp\node_modules" (
        echo     node_modules missing - npm install ...
        pushd "%ROOT%\miniapp"
        call npm install --legacy-peer-deps
        popd
    )
    start "MiniApp-H5" /D "%ROOT%\miniapp" cmd /k "npm run dev:h5"
    echo     started in new window MiniApp-H5
)

echo.
echo ============================================
echo  Waiting for services ...
echo ============================================
call :wait_port 8080 90
call :wait_port 8081 60
call :wait_port 10086 120

set "MES_WEB=http://localhost"
call :port_open 80
if errorlevel 1 (
    call :port_open 81
    if not errorlevel 1 set "MES_WEB=http://localhost:81"
)

echo.
echo ============================================================
echo   All services started. URLs:
echo.
echo   MES web UI        : %MES_WEB%
echo   MES backend API   : http://localhost:8080
echo.
echo   MiniApp H5        : http://localhost:10086
echo   MiniApp API       : http://localhost:8081/api/v1/health
echo.
echo   WeChat devtools   : open miniapp folder, run npm run build:weapp after changes
echo ============================================================
echo.
pause
exit /b 0

rem ---------- helpers ----------
:port_open
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -Command "try { $c = New-Object Net.Sockets.TcpClient; $c.Connect('127.0.0.1', %1); $c.Close(); exit 0 } catch { exit 1 }"
exit /b

:wait_port
set /a tries=%2
:wait_port_loop
call :port_open %1
if not errorlevel 1 (
    echo     port %1 ready
    exit /b 0
)
set /a tries=%tries%-1
if %tries% leq 0 (
    echo     [WARN] port %1 still not reachable, continue anyway ...
    exit /b 1
)
rem ping works even when stdin is redirected (timeout fails in that case)
ping -n 2 127.0.0.1 >nul
goto :wait_port_loop
