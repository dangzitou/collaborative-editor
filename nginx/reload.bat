@echo off
chcp 65001 >nul
echo 正在重新加载 Nginx 配置...
nginx.exe -s reload
echo [OK] 配置已重新加载
pause

