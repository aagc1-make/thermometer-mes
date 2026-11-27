@echo off
rem Start the MES voice service in its own minimized window.
rem Called from start-backend.bat / start-frontend.bat so the digital human always
rem speaks with the Azure (Xiaoyi) voice. A stale instance is stopped by the service
rem itself through free_listen_port(), so starting again is safe.
setlocal
set "HERE=%~dp0"
set "PORT=12394"
if not "%MES_AVATAR_PORT%"=="" set "PORT=%MES_AVATAR_PORT%"

if not exist "%HERE%azure-key.txt" (
  echo [voice] azure-key.txt missing -^> voice service not started.
  exit /b 0
)

echo [voice] starting digital-human voice ^(Azure, zh-CN-XiaoyiNeural^) ...
start "MES-Voice" /min cmd /c ""%HERE%start-voice.bat""
ping -n 5 127.0.0.1 >nul

netstat -ano | findstr ":%PORT% " | findstr LISTENING >nul 2>nul
if not errorlevel 1 exit /b 0
echo [voice] WARN: voice service is not listening on %PORT%.
echo [voice] Run scripts\mes-avatar-speech\start-voice.bat once to see the error.
exit /b 0
