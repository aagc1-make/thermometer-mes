@echo off
setlocal EnableExtensions EnableDelayedExpansion
title MES digital human voice
set "HERE=%~dp0"
set "LIB=%HERE%..\..\.venv-avatar\lib"
set "PYTHONPATH=%LIB%"
if "%MES_AVATAR_ENGINE%"=="" (
  if exist "%HERE%azure-key.txt" (
    set "MES_AVATAR_ENGINE=azure"
  ) else (
    set "MES_AVATAR_ENGINE=edge"
  )
)
if /i "%MES_AVATAR_ENGINE%"=="azure" if exist "%HERE%azure-key.txt" (
  for /f "usebackq tokens=1,* delims==" %%A in ("%HERE%azure-key.txt") do (
    if /i "%%A"=="region" set "MES_AVATAR_AZURE_REGION=%%B"
    if /i "%%A"=="key"    set "MES_AVATAR_AZURE_KEY=%%B"
  )
)
if "%MES_AVATAR_VOICE%"==""  set "MES_AVATAR_VOICE=zh-CN-XiaoyiNeural"
if "%MES_AVATAR_RATE%"==""   set "MES_AVATAR_RATE=-5%%"
if "%MES_AVATAR_PITCH%"==""  set "MES_AVATAR_PITCH=+8Hz"
if "%MES_AVATAR_PORT%"==""   set "MES_AVATAR_PORT=12394"

echo ============================================================
echo  MES voice service
echo    engine : %MES_AVATAR_ENGINE%
echo    voice  : %MES_AVATAR_VOICE%  rate=%MES_AVATAR_RATE%  pitch=%MES_AVATAR_PITCH%
echo    port   : %MES_AVATAR_PORT%
echo ============================================================
echo.

rem --- 1. find a usable python 3.11 ---
set "PY="
if exist "%LOCALAPPDATA%\Programs\Python\Python311\python.exe" set "PY=%LOCALAPPDATA%\Programs\Python\Python311\python.exe"
if not defined PY (
  for /f "delims=" %%P in ('where python 2^>nul') do if not defined PY set "PY=%%P"
)
if not defined PY (
  echo [voice] ERROR: python not found. Install Python 3.11, or edit PY in this file.
  goto failed
)

rem --- 2. check dependencies ---
if not exist "%LIB%\edge_tts" (
  echo [voice] ERROR: dependencies missing at %LIB%
  echo [voice] Run this once, from the repo root:
  echo         python -m pip install --target .venv-avatar\lib -r scripts\mes-avatar-speech\requirements.txt
  goto failed
)

rem --- 3. run (the service stops an old instance holding the port by itself) ---
echo [voice] starting... keep this window open. Press Ctrl+C to stop.
echo.
"%PY%" "%HERE%service.py"
echo.
echo [voice] service exited with code %ERRORLEVEL%.
echo [voice] If it says "address already in use" or "error while attempting to bind":
echo         close the old voice window, then run this file again.
:failed
echo.
echo Press any key to close this window.
pause >nul
