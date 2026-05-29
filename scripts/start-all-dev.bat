@echo off
echo ==========================================
echo Starting CoDoc Collaborative Editor
echo ==========================================

:: 1. Start Nginx
echo [1/5] Starting Nginx...
cd /d "%~dp0..\nginx"
tasklist /FI "IMAGENAME eq nginx.exe" 2>NUL | find /I /N "nginx.exe">NUL
if "%ERRORLEVEL%"=="0" (
    echo Nginx is already running. Reloading...
    nginx -s reload
) else (
    start nginx
)

:: 2. Start Yjs WebSocket Server
echo [2/5] Starting Yjs WebSocket Server...
cd /d "%~dp0..\server\yjs-server"
if not exist "node_modules" (
    echo Installing yjs-server dependencies...
    call npm install
)
start "Yjs WebSocket Server" node server.cjs

:: 3. Start Backend
echo [3/5] Starting Backend Server...
cd /d "%~dp0..\server"
start "CoDoc Backend" mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Dfile.encoding=GBK"

:: 4. Start Frontend
echo [4/5] Starting Frontend Dev Server...
cd /d "%~dp0..\web"
start "CoDoc Frontend" npm run dev

:: 5. Open Browser
echo [5/5] Opening Browser...
timeout /t 5 >nul
start http://localhost

echo ==========================================
echo All services started!
echo - Yjs WebSocket: ws://localhost:1234
echo - Backend: http://localhost:8080
echo - Frontend: http://localhost:5173
echo - Nginx: http://localhost
echo ==========================================
pause
