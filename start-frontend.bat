@echo off
chcp 65001 >nul
rem 数字人语音服务（Azure 晓伊），已在跑就跳过
call "%~dp0scripts\mes-avatar-speech\start-voice-background.bat"
cd /d "%~dp0ktg-mes-ui"

echo ============================================
echo  ktg-mes-ui dev server
echo  http://localhost:8095
echo ============================================
call npm run dev -- --port 8095

echo.
echo frontend stopped. press any key to close.
pause >nul
