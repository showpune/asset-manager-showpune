# deploy.ps1 – Deploy Asset Manager Azure infrastructure using Azure CLI
# Usage:
#   .\infra\deploy.ps1 [-ResourceGroup <rg>] [-Location <region>] [-Environment <dev|test|prod>]
#
# Prerequisites:
#   - Azure CLI installed and logged in (az login)
#   - POSTGRES_ADMIN_PASSWORD environment variable set, or entered interactively

[CmdletBinding()]
param(
    [string]$ResourceGroup  = $env:RESOURCE_GROUP   ?? "asset-manager-rg",
    [string]$Location       = $env:LOCATION          ?? "eastus",
    [string]$Environment    = $env:ENVIRONMENT_NAME  ?? "dev"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# ── PostgreSQL password ───────────────────────────────────────────────────────
$PostgresPassword = $env:POSTGRES_ADMIN_PASSWORD
if (-not $PostgresPassword) {
    $SecurePassword  = Read-Host "Enter PostgreSQL admin password" -AsSecureString
    $BSTR            = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecurePassword)
    $PostgresPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
}

Write-Host "============================================================"
Write-Host " Asset Manager – Azure Infrastructure Deployment"
Write-Host "============================================================"
Write-Host " Resource Group : $ResourceGroup"
Write-Host " Location       : $Location"
Write-Host " Environment    : $Environment"
Write-Host "============================================================"

# ── Ensure resource group exists ─────────────────────────────────────────────
Write-Host "[1/3] Ensuring resource group '$ResourceGroup' exists..."
az group create `
    --name $ResourceGroup `
    --location $Location `
    --output none

# ── Validate the template ─────────────────────────────────────────────────────
Write-Host "[2/3] Validating Bicep template..."
az deployment group validate `
    --resource-group $ResourceGroup `
    --template-file "$ScriptDir\main.bicep" `
    --parameters "$ScriptDir\parameters.json" `
    --parameters `
        location=$Location `
        environmentName=$Environment `
        postgresAdminPassword=$PostgresPassword `
    --output none

# ── Deploy ───────────────────────────────────────────────────────────────────
Write-Host "[3/3] Deploying infrastructure..."
$DeployOutput = az deployment group create `
    --resource-group $ResourceGroup `
    --template-file "$ScriptDir\main.bicep" `
    --parameters "$ScriptDir\parameters.json" `
    --parameters `
        location=$Location `
        environmentName=$Environment `
        postgresAdminPassword=$PostgresPassword `
    --output json | ConvertFrom-Json

# ── Print outputs ─────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "============================================================"
Write-Host " Deployment complete – infrastructure outputs:"
Write-Host "============================================================"
$DeployOutput.properties.outputs.PSObject.Properties | ForEach-Object {
    Write-Host ("  {0}: {1}" -f $_.Name, $_.Value.value)
}
Write-Host "============================================================"
