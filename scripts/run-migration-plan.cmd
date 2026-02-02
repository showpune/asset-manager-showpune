@echo off
REM Script to run the AppMod migration planning command

setlocal enabledelayedexpansion

set "APPMOD_BIN=%~dp0..\appmod"

echo ================================================
echo AppMod Migration Planning
echo ================================================
echo.

REM Check if appmod is installed
if not exist "%APPMOD_BIN%" (
    echo Error: AppMod CLI tool is not installed.
    echo Please run: scripts\setup-appmod.cmd
    exit /b 1
)

echo Project: Asset Manager (Java Spring Boot^)
echo Migration Target: Azure
echo.
echo Running migration plan creation...
echo.

REM Change to project root to run appmod
cd /d "%~dp0.."

REM Run the appmod command with the specified plan
"%APPMOD_BIN%" -- plan create "Create a plan to migrate the project to Azure. I don't want to upgrade my Java this time"

echo.
echo ================================================
echo Checkmark Migration plan creation completed
echo ================================================
echo.
echo Check the output above for the generated migration plan.
echo The plan should be saved in the .appmod directory or similar location.

endlocal
