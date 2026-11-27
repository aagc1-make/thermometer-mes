@echo off
setlocal EnableExtensions
chcp 65001 >nul
title MES digital human voice (Azure)
set "HERE=%~dp0"

if not exist "%HERE%azure-key.txt" (
  echo [voice] ERROR: azure-key.txt not found next to this file.
  echo [voice] Create it with two lines:
  echo         region=eastasia
  echo         key=your-subscription-key
  goto failed
)

for /f "usebackq tokens=1,* delims==" %%A in ("%HERE%azure-key.txt") do (
  if /i "%%A"=="region" set "MES_AVATAR_AZURE_REGION=%%B"
  if /i "%%A"=="key"    set "MES_AVATAR_AZURE_KEY=%%B"
)

if "%MES_AVATAR_AZURE_KEY%"=="" (
  echo [voice] ERROR: no key= line in azure-key.txt
  goto failed
)
if "%MES_AVATAR_AZURE_REGION%"=="" set "MES_AVATAR_AZURE_REGION=eastasia"

set "MES_AVATAR_ENGINE=azure"
echo [voice] using Azure Speech, region %MES_AVATAR_AZURE_REGION%
call "%HERE%start-voice.bat"
exit /b 0

:failed
echo.
echo Press any key to close.
pause >nul
