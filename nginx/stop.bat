@echo off
chcp 65001 >nul
echo 正在停止 Nginx...
nginx.exe -s stop
echo [OK] Nginx 已停止
pause

