@echo off
chcp 65001 >nul
cd /d "%~dp0ktg-mes-ui"

echo ============================================
echo  ktg-mes-ui dev server
echo  http://localhost:8095
echo ============================================
call npm run dev -- --port 8095

echo.
echo frontend stopped. press any key to close.
pause >nul
