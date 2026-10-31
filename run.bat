@echo off
setlocal
echo ===================================================
echo   Launching Apple Music Desktop Player
echo ===================================================

if exist target\apple-music-player-1.0.0.jar (
    java -jar target\apple-music-player-1.0.0.jar
    goto end
)

if exist target\classes\com\musicplayer\Main.class (
    java -cp target\classes com.musicplayer.Main
    goto end
)

where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java runtime not found. Please install JRE/JDK to run.
    pause
    exit /b 1
)

echo No compiled target found. Attempting to run directly or build...
call build.bat
if exist target\classes\com\musicplayer\Main.class (
    java -cp target\classes com.musicplayer.Main
)

:end
pause
