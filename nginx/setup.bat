@echo off
chcp 65001 >nul
echo ========================================
echo   CoDoc Nginx 安装脚本
echo ========================================
echo.

:: 检查是否已有 nginx.exe
if exist "nginx.exe" (
    echo [OK] nginx.exe 已存在
    goto :copy_dist
)

echo [提示] 请按以下步骤操作：
echo.
echo 1. 访问 http://nginx.org/en/download.html
echo 2. 下载 Windows 版本 (nginx/windows-x.x.x.zip)
echo 3. 解压后，将 nginx.exe 复制到当前 nginx 目录
echo.
echo 需要的结构：
echo    nginx/
echo    ├── nginx.exe      (从下载的 zip 中复制)
echo    ├── conf/
echo    │   ├── nginx.conf (已存在)
echo    │   └── mime.types (从下载的 zip/conf 中复制)
echo    └── html/          (前端文件目录)
echo.
pause
goto :end

:copy_dist
:: 检查 mime.types
if not exist "conf\mime.types" (
    echo [错误] 缺少 conf/mime.types 文件
    echo 请从 nginx 下载包的 conf 目录复制 mime.types 到 conf 目录
    pause
    goto :end
)

:: 复制前端构建文件
echo.
echo [INFO] 正在复制前端构建文件...
if exist "..\web\dist" (
    xcopy /E /Y /I "..\web\dist\*" "html\" >nul
    echo [OK] 前端文件已复制到 html 目录
) else (
    echo [警告] 未找到 web/dist 目录
    echo 请先运行: cd web ^&^& npm run build
)

echo.
echo ========================================
echo   安装完成！
echo ========================================
echo.
echo 启动方式：双击 nginx.exe 或 start.bat
echo 访问地址：http://localhost
echo 停止方式：双击 stop.bat
echo.
pause

:end

