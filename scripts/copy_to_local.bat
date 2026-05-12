@echo off
setlocal enabledelayedexpansion

echo.
echo ===================================================
echo   COPY PLUGIN TO LOCAL SERVER (gnhopper)
echo ===================================================
echo.

:: Get root directory (parent of scripts/)
set "ROOT_DIR=%~dp0.."
pushd "%ROOT_DIR%"
set "ROOT_DIR=%cd%"
popd

:: Paths
set "SOURCE_JAR=%ROOT_DIR%\bootstrap\paper\build\libs\gnhopper.jar"

:: Try to get TARGET_DIR from .env
set "TARGET_DIR="
if exist "%ROOT_DIR%\.env" (
    for /f "usebackq tokens=1,2 delims==" %%a in ("%ROOT_DIR%\.env") do (
        if "%%a"=="LOCAL_SERVER_PLUGINS" set "TARGET_DIR=%%b"
    )
)

:: Fallback
if "%TARGET_DIR%"=="" set "TARGET_DIR=C:\Users\lenovo\Desktop\Server\plugins"

if not exist "%SOURCE_JAR%" (
    echo [Loi] Khong tim thay file %SOURCE_JAR%
    echo Vui long chay gradlew shadowJar truoc.
    pause
    exit /b 1
)

if not exist "%TARGET_DIR%" (
    echo [Loi] Khong tim thay thu muc server: %TARGET_DIR%
    pause
    exit /b 1
)

echo Dang copy file...

:: Try to clean up Paper remapped cache
if exist "%TARGET_DIR%\.paper-remapped\gnhopper.jar" (
    del /f /q "%TARGET_DIR%\.paper-remapped\gnhopper.jar" > nul 2>&1
)

copy /Y "%SOURCE_JAR%" "%TARGET_DIR%" > nul

if %errorlevel% neq 0 (
    echo [Loi] Khong the copy file. Vui long kiem tra lai duong dan hoac quyen truy cap.
) else (
    echo [Thanh cong] Da copy gnhopper.jar sang thu muc server!
)

echo.
pause
