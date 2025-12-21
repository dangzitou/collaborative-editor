@echo off
cd /d "%~dp0..\web"
echo Building Frontend...
npm run build
echo Build Complete.
pause