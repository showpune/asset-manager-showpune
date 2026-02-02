@echo off
REM Script to set up Azure migration planning tools
REM This script performs the following:
REM 1. Installs GitHub Copilot CLI
REM 2. Downloads and installs the appmod tool
REM 3. Creates an Azure migration plan

setlocal enabledelayedexpansion

echo ==========================================
echo Azure Migration Planning Setup
echo ==========================================
echo.

REM Step 1: Install GitHub Copilot CLI
echo Step 1: Installing GitHub Copilot CLI...
echo Running: curl -fsSL https://gh.io/copilot-install ^| bash
echo Note: This step requires Git Bash or WSL on Windows
echo Please run the following command manually in Git Bash or WSL:
echo   curl -fsSL https://gh.io/copilot-install ^| bash
echo.
pause
echo.

REM Step 2: Download and install appmod tool
echo Step 2: Downloading appmod tool...
set APPMOD_URL=https://aka.ms/appmod_win-x64.zip
set APPMOD_DIR=%TEMP%\appmod
set APPMOD_ZIP=%TEMP%\appmod_win-x64.zip

REM Create temporary directory
if not exist "%APPMOD_DIR%" mkdir "%APPMOD_DIR%"

REM Download the appmod tool
echo Downloading from %APPMOD_URL%...
curl -L -o "%APPMOD_ZIP%" "%APPMOD_URL%"

if not exist "%APPMOD_ZIP%" (
    echo Error: Failed to download appmod tool
    exit /b 1
)

REM Extract the tool (requires PowerShell)
echo Extracting appmod tool...
powershell -Command "Expand-Archive -Path '%APPMOD_ZIP%' -DestinationPath '%APPMOD_DIR%' -Force"

REM Find the appmod binary
set APPMOD_BIN=%APPMOD_DIR%\appmod.exe

if not exist "%APPMOD_BIN%" (
    echo Error: Could not find appmod.exe in the extracted files
    echo Looking for appmod in subdirectories...
    for /r "%APPMOD_DIR%" %%F in (appmod.exe) do (
        set APPMOD_BIN=%%F
        goto :found
    )
    echo Error: appmod.exe not found
    exit /b 1
)

:found
echo appmod tool installed successfully.
echo Location: %APPMOD_BIN%
echo.

REM Step 3: Run appmod plan creation
echo Step 3: Creating Azure migration plan...
echo Running: appmod plan create "Create a plan to migrate the project to Azure. I don't want to upgrade my Java this time"
"%APPMOD_BIN%" plan create "Create a plan to migrate the project to Azure. I don't want to upgrade my Java this time"

echo.
echo ==========================================
echo Setup complete!
echo ==========================================
echo.
echo Note: The appmod tool is located at: %APPMOD_BIN%
echo To use it in other terminal sessions, add it to your PATH.

endlocal
