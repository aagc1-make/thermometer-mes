@echo off
setlocal

rem ============================================================
rem  MES demo backend on port 8081 (database: j2eedb_it)
rem
rem  Why this script:
rem    The frontend dev server (port 80) proxies /dev-api to
rem    http://localhost:8081 (see ktg-mes-ui\vue.config.js).
rem    If only the 8080 instance runs, the browser console shows
rem    "Proxy error ... ECONNREFUSED" and login captcha fails.
rem
rem  This starts the same instance used by the demo:
rem    --server.port=8081  +  j2eedb_it datasource
rem ============================================================

set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_261"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MYSQL_HOME=D:\mysql-8.0.44-winx64"
set "REDIS_HOME=E:\dev\redis"
set "PROJECT=%~dp0backend\ktg-mes"
set "JAR=%PROJECT%\ktg-admin\target\ktg-admin.jar"
rem 旧的临时构建包（ai-analysis / calendar-fix）缺少后续新增接口，
rem 例如一键拣货 POST /mes/wm/issueheader/{id}/auto-pick，使用前请重新
rem 用 mvn -DskipTests -pl ktg-admin -am package 打最新的 ktg-admin.jar。

call :port_open 8081
if not errorlevel 1 (
    echo [INFO] port 8081 is already running, nothing to do.
    echo        To restart: end the java.exe process, then run this again.
    exit /b 0
)

echo ============================================
echo  [1/3] MySQL :3307
echo ============================================
call :port_open 3307
if errorlevel 1 (
    echo     not running -^> starting mysqld ...
    start "MES-MySQL" /min "%MYSQL_HOME%\bin\mysqld.exe" --defaults-file=%MYSQL_HOME%\my.ini
    call :wait_port 3307 30
) else (
    echo     already listening -^> skip
)

echo ============================================
echo  [2/3] Redis :6379  ^(no password^)
echo ============================================
call :port_open 6379
if errorlevel 1 (
    echo     not running -^> starting redis-server ...
    pushd "%REDIS_HOME%"
    start "MES-Redis" /min "%REDIS_HOME%\redis-server.exe" redis.windows.conf
    popd
    call :wait_port 6379 15
) else (
    echo     already listening -^> skip
)

echo ============================================
echo  [3/3] Backend :8081  ^(db=j2eedb_it, proxy target of frontend :80^)
echo ============================================
if not exist "%JAR%" (
    echo     [ERROR] jar not found: %JAR%
    echo     build it first:
    echo         cd /d %PROJECT%
    echo         mvn -DskipTests -pl ktg-admin -am package
    pause
    exit /b 1
)

cd /d "%PROJECT%\ktg-admin\target"
rem Credentials come from the ignored application-druid.yml or MES_DB_* environment variables.
if not defined MES_DB_URL set "MES_DB_URL=jdbc:mysql://localhost:3307/j2eedb_it?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%%2B8"
"%JAVA_HOME%\bin\java.exe" -Dfile.encoding=UTF-8 -jar "%JAR%" --server.port=8081 --spring.datasource.druid.master.url="%MES_DB_URL%"

echo.
echo backend(8081) stopped. press any key to close.
pause >nul
exit /b 0

rem ---------- helpers ----------
:port_open
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -Command "try { $c = New-Object Net.Sockets.TcpClient; $c.Connect('127.0.0.1', %1); $c.Close(); exit 0 } catch { exit 1 }"
exit /b

:wait_port
set /a tries=%2
:wait_port_loop
call :port_open %1
if not errorlevel 1 exit /b 0
set /a tries=%tries%-1
if %tries% leq 0 (
    echo     [WARN] port %1 still not reachable, continue anyway ...
    exit /b 1
)
timeout /t 1 /nobreak >nul
goto :wait_port_loop
