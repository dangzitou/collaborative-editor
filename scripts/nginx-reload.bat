@echo off
cd /d "%~dp0..\nginx"
echo Reloading Nginx...
nginx -s reload
echo Nginx Reloaded.
pause