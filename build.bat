@echo off
setlocal
echo ===================================================
echo   Building Apple Music Desktop Player
echo ===================================================

if exist pom.xml (
    where mvn >nul 2>nul
    if %ERRORLEVEL% equ 0 (
        echo Running Maven Package...
        call mvn clean package
        goto done
    )
)

where javac >nul 2>nul
if %ERRORLEVEL% equ 0 (
    echo Compiling with javac...
    if not exist target\classes mkdir target\classes
    javac -encoding UTF-8 -d target\classes -sourcepath src\main\java src\main\java\com\musicplayer\Main.java
    echo Compilation succeeded!
    goto done
)

echo [INFO] Neither 'mvn' nor 'javac' was detected on your PATH.
echo Please install JDK 17+ or ensure javac is added to PATH.

:done
echo Done.
pause
