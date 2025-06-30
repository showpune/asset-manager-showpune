@echo off
REM Asset Manager Configuration Script
REM Use this script to easily switch between storage providers

if "%1"=="--azure" goto set_azure
if "%1"=="--s3" goto set_s3
if "%1"=="--dev" goto set_dev
if "%1"=="--status" goto show_status
if "%1"=="--help" goto show_help
goto invalid_option

:set_azure
echo Setting Spring Boot profile to: azure
call :update_profile azure
echo Configuration updated successfully!
echo.
echo Now configured for Azure Blob Storage and Service Bus
echo Make sure to set these environment variables:
echo   set AZURE_CLIENT_ID=your-managed-identity-client-id
echo   set SERVICE_BUS_NAMESPACE=your-servicebus-namespace.servicebus.windows.net
goto end

:set_s3
echo Setting Spring Boot profile to: s3
call :update_profile s3
echo Configuration updated successfully!
echo.
echo Now configured for AWS S3 and RabbitMQ
echo Make sure to configure your AWS credentials in application-s3.properties
goto end

:set_dev
echo Setting Spring Boot profile to: dev
call :update_profile dev
echo Configuration updated successfully!
echo.
echo Now configured for local development (local file system + RabbitMQ)
echo Use Docker to start RabbitMQ and PostgreSQL services
goto end

:show_status
echo Current Configuration Status:
echo.
if exist "web\src\main\resources\application.properties" (
    findstr "spring.profiles.active" web\src\main\resources\application.properties > nul
    if errorlevel 1 (
        echo Web module profile: default
    ) else (
        for /f "tokens=2 delims==" %%a in ('findstr "spring.profiles.active" web\src\main\resources\application.properties') do echo Web module profile: %%a
    )
)
if exist "worker\src\main\resources\application.properties" (
    findstr "spring.profiles.active" worker\src\main\resources\application.properties > nul
    if errorlevel 1 (
        echo Worker module profile: default
    ) else (
        for /f "tokens=2 delims==" %%a in ('findstr "spring.profiles.active" worker\src\main\resources\application.properties') do echo Worker module profile: %%a
    )
)
echo.
echo Available profiles:
echo   - azure: Azure Blob Storage + Azure Service Bus
echo   - s3: AWS S3 + RabbitMQ
echo   - dev: Local file system + RabbitMQ (default)
goto end

:show_help
echo Asset Manager Configuration Script
echo.
echo Usage: %0 [OPTION]
echo.
echo Options:
echo   --azure     Configure for Azure Blob Storage and Service Bus
echo   --s3        Configure for AWS S3 and RabbitMQ
echo   --dev       Configure for local file system and RabbitMQ (development)
echo   --status    Show current configuration
echo   --help      Show this help message
echo.
echo Examples:
echo   %0 --azure    # Switch to Azure configuration
echo   %0 --s3       # Switch to AWS S3 configuration
echo   %0 --dev      # Switch to development configuration
goto end

:invalid_option
echo Error: Invalid option '%1'
echo.
goto show_help

:update_profile
set profile=%1
REM Update web application
if exist "web\src\main\resources\application.properties" (
    findstr /v "spring.profiles.active" web\src\main\resources\application.properties > temp_web.properties
    echo spring.profiles.active=%profile% >> temp_web.properties
    move temp_web.properties web\src\main\resources\application.properties > nul
)

REM Update worker application  
if exist "worker\src\main\resources\application.properties" (
    findstr /v "spring.profiles.active" worker\src\main\resources\application.properties > temp_worker.properties
    echo spring.profiles.active=%profile% >> temp_worker.properties
    move temp_worker.properties worker\src\main\resources\application.properties > nul
)
goto :eof

:end