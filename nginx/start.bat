@echo off
chcp 65001 >nul
echo 正在启动 CoDoc 前端服务...

:: 检查 nginx.exe
if not exist "nginx.exe" (
    echo [错误] 未找到 nginx.exe
    echo 请先运行 setup.bat 进行安装
    pause
    exit /b 1
)

:: 检查是否已运行
tasklist /FI "IMAGENAME eq nginx.exe" 2>NUL | find /I /N "nginx.exe">NUL
if "%ERRORLEVEL%"=="0" (
    echo [提示] Nginx 已在运行中
    echo 访问地址：http://localhost
    pause
    exit /b 0
)

:: 启动 nginx
start nginx.exe
echo.
echo [OK] Nginx 已启动
echo 访问地址：http://localhost
echo.
echo 按任意键打开浏览器...
pause >nul
start http://localhost

