@echo off
chcp 65001 >nul
setlocal

rem ============================================================
rem  MES 知识图谱 Neo4j 启动器
rem  需要 JDK 21（Neo4j 5.x 的运行要求），与业务后端的 JDK 8 分开指定。
rem ============================================================

if not defined NEO4J_HOME set "NEO4J_HOME=E:\dev\neo4j"
if not defined NEO4J_JAVA set "NEO4J_JAVA=C:\Program Files\Java\jdk-21\bin\java.exe"

if not exist "%NEO4J_HOME%\lib" (
    echo [ERROR] Neo4j not found at "%NEO4J_HOME%".
    exit /b 1
)
if not exist "%NEO4J_JAVA%" (
    echo [ERROR] JDK 21 not found at "%NEO4J_JAVA%".
    exit /b 1
)

cd /d "%NEO4J_HOME%"
"%NEO4J_JAVA%" -Dneo4j.home="%NEO4J_HOME%" -cp "lib/*" org.neo4j.server.startup.Neo4jCommand console
