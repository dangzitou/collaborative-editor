@echo off
echo ==========================================
echo Starting Collaborative Editor Environment
echo ==========================================

:: 1. Start Nginx
echo [1/4] Starting Nginx...
cd /d "%~dp0..\nginx"
:: Check if nginx is already running to avoid errors, or just try to start
tasklist /FI "IMAGENAME eq nginx.exe" 2>NUL | find /I /N "nginx.exe">NUL
if "%ERRORLEVEL%"=="0" (
    echo Nginx is already running. Reloading...
    nginx -s reload
) else (
    start nginx
)

:: 2. Start Backend
echo [2/4] Starting Backend Server...
cd /d "%~dp0..\server"
start "Collaborative Editor Backend" mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Dfile.encoding=GBK"

:: 3. Start Frontend
echo [3/4] Starting Frontend Dev Server...
cd /d "%~dp0..\web"
start "Collaborative Editor Frontend" npm run dev

:: 4. Open Browser
echo [4/4] Opening Browser...
:: Wait a bit for services to spin up
timeout /t 5 >nul
start http://localhost

echo ==========================================
echo All services started!
echo Backend running in new window.
echo Frontend running in new window.
echo Nginx running in background.
echo ==========================================
pause