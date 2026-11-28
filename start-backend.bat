@echo off
chcp 65001 >nul
setlocal

rem Load the current Windows user key if the launching shell predates its setup.
if not defined MES_FACE_TEMPLATE_KEY for /f "tokens=1,2,*" %%A in ('"%SystemRoot%\System32\reg.exe" query HKCU\Environment /v MES_FACE_TEMPLATE_KEY 2^>nul') do if /i "%%A"=="MES_FACE_TEMPLATE_KEY" set "MES_FACE_TEMPLATE_KEY=%%C"

rem ============================================================
rem  Infrared thermometer MES - backend one-click start
rem  Requires: MySQL 3307 / Redis 6379 / MinIO 9100 / JDK8
rem        Neo4j 7474 graph db, JDK21, install dir E:/dev/neo4j
rem  Redis runs without password here; never add --requirepass
rem ============================================================

set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_261"
set "PATH=%JAVA_HOME%\bin;%PATH%"
if not defined MYSQL_HOME set "MYSQL_HOME=E:\D\mysql-8.0.44-winx64"
set "REDIS_HOME=E:\dev\redis"
set "MINIO_HOME=E:\dev\minio"
set "NEO4J_HOME=E:\dev\neo4j"
set "NEO4J_JAVA=C:\Program Files\Java\jdk-21\bin\java.exe"
set "PROJECT=%~dp0backend\ktg-mes"

if not defined SERVER_PORT set "MES_BACKEND_PORT=8080"
if defined SERVER_PORT set "MES_BACKEND_PORT=%SERVER_PORT%"
call :port_open %MES_BACKEND_PORT%
if not errorlevel 1 (
    echo [ERROR] Port %MES_BACKEND_PORT% is already in use. Check the running backend before starting another instance.
    exit /b 1
)

rem  ai-keys.txt (gitignored) holds model keys and local service configuration
rem  Without that file the assistant stays on the native path
if exist "%~dp0ai-keys.txt" (
    for /f "usebackq tokens=1,* delims==" %%A in ("%~dp0ai-keys.txt") do (
        if /i "%%A"=="MES_AI_KEY"      set "MES_AI_KEY=%%B"
        if /i "%%A"=="MES_AI_FB_KEY"   set "MES_AI_FB_KEY=%%B"
        if /i "%%A"=="MES_AI_DIFY_URL" set "MES_AI_DIFY_URL=%%B"
        if /i "%%A"=="MES_AI_DIFY_KEY" set "MES_AI_DIFY_KEY=%%B"
        if /i "%%A"=="MES_AI_DIFY_CONSOLE_WORKFLOW_URL" if not defined MES_AI_DIFY_CONSOLE_WORKFLOW_URL set "MES_AI_DIFY_CONSOLE_WORKFLOW_URL=%%B"
        if /i "%%A"=="MES_AI_NEO4J_URL" if not defined MES_AI_NEO4J_URL set "MES_AI_NEO4J_URL=%%B"
        if /i "%%A"=="MES_AI_NEO4J_USER" if not defined MES_AI_NEO4J_USER set "MES_AI_NEO4J_USER=%%B"
        if /i "%%A"=="MES_AI_NEO4J_PASSWORD" if not defined MES_AI_NEO4J_PASSWORD set "MES_AI_NEO4J_PASSWORD=%%B"
    )
)
if not defined MES_AI_NEO4J_URL set "MES_AI_NEO4J_URL=http://127.0.0.1:7474"
if not defined MES_AI_NEO4J_USER set "MES_AI_NEO4J_USER=neo4j"
if defined MES_AI_DIFY_URL if defined MES_AI_DIFY_KEY (
    echo [ai] Dify workflow: %MES_AI_DIFY_URL%
) else (
    echo [ai] Dify workflow: not configured ^(仅使用 MES 原生问答^)
)

rem  Digital-human voice service (Azure Xiaoyi); skipped when running
call "%~dp0scripts\mes-avatar-speech\start-voice-background.bat"

echo ============================================
echo  [1/4] MySQL  :3307
echo ============================================
call :port_open 3307
if errorlevel 1 (
    echo     not running -^> starting mysqld ...
    start "MES-MySQL" /min "%MYSQL_HOME%\bin\mysqld.exe" --defaults-file="%MYSQL_HOME%\my.ini"
    call :wait_port 3307 30
    if errorlevel 1 (
        echo     [ERROR] MySQL did not start. Check "%MYSQL_HOME%\my.ini" and the MySQL error log.
        pause
        exit /b 1
    )
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
echo  [4/5] Neo4j  :7474  ^(知识图谱^)
echo ============================================
call :port_open 7474
if errorlevel 1 (
    if exist "%NEO4J_HOME%\lib" (
        echo     not running -^> starting neo4j ...
        start "MES-Neo4j" /min "%~dp0scripts\start-neo4j.bat"
        call :wait_port 7474 60
        if errorlevel 1 (
            echo     [WARN] Neo4j did not start; graph features will show as unavailable.
        )
    ) else (
        echo     [WARN] "%NEO4J_HOME%" not found -^> graph features unavailable.
    )
) else (
    echo     already listening -^> skip
)

echo ============================================
echo  [5/5] Backend :%MES_BACKEND_PORT%
echo ============================================
if not defined MES_BACKEND_JAR set "MES_BACKEND_JAR=%PROJECT%\ktg-admin\target\ktg-admin.jar"
if not exist "%MES_BACKEND_JAR%" (
    echo     [ERROR] jar not found. build it first:
    echo         cd /d %PROJECT%
    echo         mvn -DskipTests -pl ktg-admin -am package
    echo     ^(Maven must run with JAVA_HOME=JDK8^)
    pause
    exit /b 1
)
rem Pass Dify via system properties as well; this is the proven local path and
rem takes precedence over any Spring JSON environment configuration.
set "MES_DIFY_OPTS="
set "MES_DIFY_CONSOLE_OPT="
if defined MES_AI_DIFY_CONSOLE_WORKFLOW_URL set MES_DIFY_CONSOLE_OPT="-Dmes.ai.dify.console-workflow-url=%MES_AI_DIFY_CONSOLE_WORKFLOW_URL%"
if defined MES_AI_DIFY_URL if defined MES_AI_DIFY_KEY set "MES_DIFY_OPTS=-Dmes.ai.dify.api-base-url=%MES_AI_DIFY_URL% -Dmes.ai.dify.api-key=%MES_AI_DIFY_KEY%"
if not exist "%PROJECT%\upload" mkdir "%PROJECT%\upload"
cd /d "%PROJECT%"
echo     java -jar "%MES_BACKEND_JAR%"
echo.
"%JAVA_HOME%\bin\java.exe" -Dfile.encoding=UTF-8 %MES_DIFY_OPTS% %MES_DIFY_CONSOLE_OPT% -jar "%MES_BACKEND_JAR%"

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
    echo     [WARN] port %1 still not reachable.
    exit /b 1
)
timeout /t 1 /nobreak >nul
goto :wait_port_loop
