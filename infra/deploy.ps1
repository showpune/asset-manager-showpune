# Asset Manager - Azure Infrastructure Deployment Script (Windows)
# Usage: .\deploy.ps1 -ResourceGroup <name> -Location <location> [-Environment <env>]
#
# Prerequisites:
#   - Azure CLI installed and logged in (az login)
#   - Sufficient permissions to create resources and role assignments
#
# Example:
#   .\deploy.ps1 -ResourceGroup rg-assetmgr-dev -Location eastus -Environment dev

param(
    [Parameter(Mandatory = $true, HelpMessage = "Azure Resource Group name")]
    [string]$ResourceGroup,

    [Parameter(Mandatory = $true, HelpMessage = "Azure region (e.g., eastus, westeurope)")]
    [string]$Location,

    [Parameter(Mandatory = $false, HelpMessage = "Environment name (e.g., dev, staging, prod)")]
    [string]$Environment = "dev"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "============================================"
Write-Host "  Asset Manager - Azure Infrastructure"
Write-Host "============================================"
Write-Host "  Resource Group : $ResourceGroup"
Write-Host "  Location       : $Location"
Write-Host "  Environment    : $Environment"
Write-Host "============================================"
Write-Host ""

# Verify Azure CLI is logged in
try {
    az account show --output none 2>$null
    if ($LASTEXITCODE -ne 0) { throw }
}
catch {
    Write-Error "Not logged in to Azure CLI. Run 'az login' first."
    exit 1
}

# Prompt for PostgreSQL admin password securely
$SecurePassword = Read-Host -Prompt "Enter PostgreSQL admin password" -AsSecureString
$PostgresPassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecurePassword)
)

if ([string]::IsNullOrEmpty($PostgresPassword)) {
    Write-Error "PostgreSQL admin password cannot be empty."
    exit 1
}

# Create resource group if it doesn't exist
Write-Host "Creating resource group '$ResourceGroup' in '$Location'..."
az group create `
    --name $ResourceGroup `
    --location $Location `
    --output none

Write-Host "Deploying infrastructure (this may take 5-10 minutes)..."
az deployment group create `
    --resource-group $ResourceGroup `
    --template-file "$ScriptDir\main.bicep" `
    --parameters "$ScriptDir\parameters.json" `
    --parameters environmentName=$Environment `
                 location=$Location `
                 postgresAdminPassword=$PostgresPassword `
    --output table

if ($LASTEXITCODE -ne 0) {
    Write-Error "Deployment failed."
    exit 1
}

Write-Host ""
Write-Host "Deployment complete! Fetching outputs..."
az deployment group show `
    --resource-group $ResourceGroup `
    --name "main" `
    --query "properties.outputs" `
    --output json
