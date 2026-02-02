@echo off
REM Script to download and setup the AppMod CLI tool

setlocal enabledelayedexpansion

set "APPMOD_URL=https://appmodcli.blob.core.windows.net/privaterelease/appmod_linux-x64.tar.gz?sp=r&st=2026-02-02T08:31:30Z&se=2026-02-28T16:46:30Z&spr=https&sv=2024-11-04&sr=b&sig=KJLpTrRs83a9MfkoIzpmDYKsYC%%2Bvqgt9uNGp39Hbzxs%%3D"
set "APPMOD_TAR=%~dp0..\appmod_linux-x64.tar.gz"
set "APPMOD_BIN=%~dp0..\appmod"

echo ================================================
echo AppMod CLI Tool Setup
echo ================================================
echo.

REM Check if appmod is already installed
if exist "%APPMOD_BIN%" (
    echo Checkmark AppMod CLI tool is already installed at: %APPMOD_BIN%
    echo.
    "%APPMOD_BIN%" --version 2>nul || echo Note: Unable to verify version
    exit /b 0
)

echo Downloading AppMod CLI tool...
echo URL: %APPMOD_URL%
echo.

REM Try to download using curl (available in Windows 10+)
where curl >nul 2>&1
if %errorlevel% equ 0 (
    curl -L "%APPMOD_URL%" -o "%APPMOD_TAR%"
    if !errorlevel! neq 0 (
        echo Error: Download failed
        exit /b 1
    )
) else (
    echo Error: curl is not available. Please install curl or download manually.
    echo.
    echo Download URL:
    echo %APPMOD_URL%
    echo.
    echo Save as: %APPMOD_TAR%
    exit /b 1
)

REM Check if download was successful
if not exist "%APPMOD_TAR%" (
    echo Error: Download failed - file not found
    exit /b 1
)

echo Checkmark Download completed
echo.

REM Note: Windows doesn't have tar by default, but Windows 10+ has it built-in
echo Extracting AppMod CLI tool...
where tar >nul 2>&1
if %errorlevel% equ 0 (
    tar -xzf "%APPMOD_TAR%" -C "%~dp0.."
    if !errorlevel! neq 0 (
        echo Error: Extraction failed
        exit /b 1
    )
) else (
    echo Error: tar command is not available.
    echo Please extract %APPMOD_TAR% manually to the project root.
    exit /b 1
)

REM Check if extraction was successful
if not exist "%APPMOD_BIN%" (
    echo Error: Extraction failed. 'appmod' binary not found.
    exit /b 1
)

echo Checkmark Extraction completed
echo.

echo Verifying installation...
"%APPMOD_BIN%" --version 2>nul
if !errorlevel! equ 0 (
    echo.
    echo ================================================
    echo Checkmark AppMod CLI tool successfully installed!
    echo ================================================
    echo.
    echo Binary location: %APPMOD_BIN%
    echo.
    echo You can now run migration planning with:
    echo   scripts\run-migration-plan.cmd
) else (
    echo Warning: Installation completed but unable to verify version
    echo Note: This tool may be for Linux. Consider using WSL or Linux environment.
)

endlocal
