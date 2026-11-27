@echo off
setlocal
chcp 65001 >nul
title Azure Speech check
set "HERE=%~dp0"
set "PYTHONPATH=%HERE%..\..\.venv-avatar\lib"
set "PY=%LOCALAPPDATA%\Programs\Python\Python311\python.exe"
if not exist "%PY%" for /f "delims=" %%P in ('where python 2^>nul') do set "PY=%%P"
if "%~1"=="" (
  echo Usage: drag nothing - run from cmd, or edit KEY below.
  echo   check-azure.bat ^<subscription-key^> [region]
  echo.
  set /p KEY=KEY:
  set /p REGION=REGION [eastasia]:
) else (
  set "KEY=%~1"
  set "REGION=%~2"
)
if "%KEY%"=="" goto done
if "%REGION%"=="" set "REGION=eastasia"
cd /d "%HERE%"
"%PY%" "%HERE%check_azure.py" "%KEY%" "%REGION%"
:done
echo.
echo Press any key to close.
pause >nul
