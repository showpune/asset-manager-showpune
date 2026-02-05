@echo off
setlocal

rem Get the directory where the script is located
set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..
set MCP_CONFIG_FILE=%PROJECT_ROOT%\.github\mcp-config.json

echo ================================================
echo        MCP Servers Configuration List          
echo ================================================
echo.

rem Check if the config file exists
if not exist "%MCP_CONFIG_FILE%" (
    echo Error: MCP configuration file not found at %MCP_CONFIG_FILE%
    goto :end
)

echo MCP Servers found in .github\mcp-config.json:
echo.

rem Try to use PowerShell for better JSON parsing
powershell -Command ^
  "if (Test-Path '%MCP_CONFIG_FILE%') { ^
    $config = Get-Content '%MCP_CONFIG_FILE%' ^| ConvertFrom-Json; ^
    $config.mcpServers.PSObject.Properties ^| ForEach-Object { ^
      Write-Host \"Server Name: $($_.Name)\"; ^
      Write-Host \"  Type: $($_.Value.type)\"; ^
      Write-Host \"  Command: $($_.Value.command)\"; ^
      Write-Host \"  Args: $($_.Value.args -join ' ')\"; ^
      Write-Host \"  Tools: $($_.Value.tools -join ', ')\"; ^
      Write-Host \"\" ^
    } ^
  }" 2>nul

if errorlevel 1 (
    echo Fallback: Displaying raw JSON content
    echo.
    type "%MCP_CONFIG_FILE%"
)

echo.
echo ================================================

:end
endlocal
