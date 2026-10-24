@echo off
chcp 65001 >nul
setlocal

rem ============================================================
rem  红外测温枪MES 后端一键启动
rem  前置：MySQL(3307) / Redis(6379, 无密码) / MinIO(9100) / JDK8
rem  说明：application.yml 中 redis 未配置密码，因此 Redis 不能再加 --requirepass，
rem        否则启动会报 "ERR Client sent AUTH, but no password is set"
rem ============================================================

set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_261"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MYSQL_HOME=D:\mysql-8.0.44-winx64"
set "REDIS_HOME=E:\dev\redis"
set "MINIO_HOME=E:\dev\minio"
set "PROJECT=E:\JavaCode\backend\ktg-mes"

echo ============================================
echo  [1/4] MySQL  :3307
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
echo  [2/4] Redis  :6379  ^(no password^)
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
echo  [3/4] MinIO  :9100  ^(bucket lx-mes^)
echo ============================================
call :port_open 9100
if errorlevel 1 (
    echo     not running -^> starting minio ...
    set "MINIO_ROOT_USER=minioadmin"
    set "MINIO_ROOT_PASSWORD=minioadmin"
    start "MES-MinIO" /min "%MINIO_HOME%\minio.exe" server "%MINIO_HOME%\data" --address :9100 --console-address ":9101"
    call :wait_port 9100 20
) else (
    echo     already listening -^> skip
)

echo ============================================
echo  [4/4] Backend :8080
echo ============================================
if not exist "%PROJECT%\ktg-admin\target\ktg-admin.jar" (
    echo     [ERROR] jar not found. build it first:
    echo         cd /d %PROJECT%
    echo         mvn -DskipTests -pl ktg-admin -am package
    echo     ^(Maven must run with JAVA_HOME=JDK8^)
    pause
    exit /b 1
)
if not exist "%PROJECT%\upload" mkdir "%PROJECT%\upload"
cd /d "%PROJECT%"
echo     java -jar ktg-admin\target\ktg-admin.jar
echo.
"%JAVA_HOME%\bin\java.exe" -Dfile.encoding=UTF-8 -jar ktg-admin\target\ktg-admin.jar

echo.
echo backend stopped. press any key to close.
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
