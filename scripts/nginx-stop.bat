@echo off
cd /d "%~dp0..\nginx"
echo Stopping Nginx...
nginx -s stop
echo Nginx Stopped.
pause