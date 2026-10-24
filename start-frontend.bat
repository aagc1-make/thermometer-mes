@echo off
chcp 65001 >nul
cd /d E:\JavaCode\ktg-mes-ui

echo ============================================
echo  ktg-mes-ui dev server
echo  default port 80; if 80 is busy vue-cli
echo  will pick the next free port (e.g. 81)
echo ============================================
call npm run dev

echo.
echo frontend stopped. press any key to close.
pause >nul
