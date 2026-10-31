@echo off
setlocal
echo ===================================================
echo   Starting Apple Music Desktop Player
echo ===================================================

where node >nul 2>nul
if %ERRORLEVEL% equ 0 (
    start http://localhost:3000
    node server.js
    goto end
)

echo [INFO] Node.js not detected. Opening player in default browser directly...
start "" "%~dp0web\index.html"

:end
pause
