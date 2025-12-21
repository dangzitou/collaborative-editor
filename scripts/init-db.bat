@echo off
setlocal

:: ====================================================
:: 数据库初始化脚本
:: 请根据你的本地环境修改下面的配置
:: ====================================================
set DB_HOST=localhost
set DB_PORT=3306
set DB_USER=root
:: 如果你的密码不是 root，请修改这里，或者留空在运行时手动输入

set /p DB_PASS="请输入数据库密码（输入时不会显示）："

echo ==========================================
echo Initialize Database (CoDoc)
echo ==========================================
echo Host: %DB_HOST%:%DB_PORT%
echo User: %DB_USER%
echo SQL File: %~dp0init.sql
echo.

:: 检查 mysql 命令是否存在
where mysql >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [Error] 'mysql' command not found.
    echo Please ensure MySQL is installed and added to your PATH environment variable.
    pause
    exit /b 1
)

echo Connecting to MySQL and executing script...
echo (If it hangs, it might be waiting for a password if configured incorrectly)


:: 执行导入（始终手动输入密码，避免明文保存）
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p < "%~dp0init.sql"

if %ERRORLEVEL% equ 0 (
    echo.
    echo [Success] Database initialized successfully!
) else (
    echo.
    echo [Failed] Error initializing database. 
    echo Please check your username, password, and if MySQL service is running.
)

pause