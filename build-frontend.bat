@echo off
chcp 65001 >nul
cd /d E:\JavaCode\ktg-mes-ui

echo ============================================================
echo  构建前端发布包（输出到 ktg-mes-ui\dist）
echo  说明：项目用的是 webpack4，在 Node 17+ 上默认会报
echo        error:0308010C:digital envelope routines::unsupported
echo        这里加 --openssl-legacy-provider 兼容，无需降级 Node。
echo ============================================================
set NODE_OPTIONS=--openssl-legacy-provider
call npm run build:prod

if errorlevel 1 (
    echo.
    echo [ERROR] 构建失败，请把上面的报错发出来。
    pause
    exit /b 1
)

echo.
echo [OK] dist 已更新：E:\JavaCode\ktg-mes-ui\dist
echo      静态服务 server.js 会按请求读取文件，无需重启；
echo      浏览器请按 Ctrl+Shift+R 强制刷新。
pause >nul
