# deploy.ps1 - Deploy Asset Manager infrastructure to Azure using Azure CLI
# Usage: .\deploy.ps1 [OPTIONS]
#
# Parameters:
#   -ResourceGroup      Resource group name (required)
#   -Location           Azure region (default: eastus)
#   -Environment        Environment name: dev|staging|prod (default: dev)
#   -PostgresPassword   PostgreSQL admin password (required)
#   -AppName            Application name (default: asset-manager)
#   -WhatIf             Perform a what-if analysis only (no deployment)

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ResourceGroup,

    [Parameter(Mandatory = $false)]
    [string]$Location = "eastus",

    [Parameter(Mandatory = $false)]
    [ValidateSet("dev", "staging", "prod")]
    [string]$Environment = "dev",

    [Parameter(Mandatory = $false)]
    [string]$PostgresPassword,

    [Parameter(Mandatory = $false)]
    [string]$AppName = "asset-manager",

    [Parameter(Mandatory = $false)]
    [switch]$WhatIf
)

$ErrorActionPreference = "Stop"
$ScriptDir = $PSScriptRoot

# Validate required parameters
if (-not $WhatIf -and -not $PostgresPassword) {
    Write-Error "PostgresPassword is required for deployment. Use -WhatIf to skip deployment."
    exit 1
}

# Ensure logged in to Azure
try {
    az account show | Out-Null
} catch {
    Write-Host "Not logged in to Azure. Running 'az login'..."
    az login
}

Write-Host "=== Asset Manager Infrastructure Deployment ===" -ForegroundColor Cyan
Write-Host "Resource Group : $ResourceGroup"
Write-Host "Location       : $Location"
Write-Host "Environment    : $Environment"
Write-Host "App Name       : $AppName"
Write-Host ""

# Create resource group if it does not exist
$rgExists = az group show --name $ResourceGroup 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Creating resource group '$ResourceGroup' in '$Location'..."
    az group create `
        --name $ResourceGroup `
        --location $Location `
        --tags "environment=$Environment" "application=$AppName" "managedBy=bicep"
}

$DeploymentName = "asset-manager-infra-$(Get-Date -Format 'yyyyMMddHHmmss')"

$CommonArgs = @(
    "--resource-group", $ResourceGroup,
    "--template-file", "$ScriptDir\main.bicep",
    "--parameters", "$ScriptDir\parameters.json",
    "--parameters",
    "location=$Location",
    "environmentName=$Environment",
    "appName=$AppName"
)

if ($WhatIf) {
    Write-Host "Running what-if analysis..." -ForegroundColor Yellow
    az deployment group what-if @CommonArgs `
        --parameters "postgresAdminPassword=$PostgresPassword"
} else {
    Write-Host "Deploying infrastructure (deployment: $DeploymentName)..." -ForegroundColor Green
    az deployment group create `
        --name $DeploymentName `
        @CommonArgs `
        --parameters "postgresAdminPassword=$PostgresPassword" `
        --output json

    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "=== Deployment Outputs ===" -ForegroundColor Cyan
        az deployment group show `
            --resource-group $ResourceGroup `
            --name $DeploymentName `
            --query "properties.outputs" `
            --output table

        Write-Host ""
        Write-Host "Deployment '$DeploymentName' completed successfully." -ForegroundColor Green
    } else {
        Write-Error "Deployment failed. Check the Azure portal for details."
        exit 1
    }
}
