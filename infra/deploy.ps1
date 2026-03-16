# deploy.ps1 – Deploy Asset Manager Kit Azure infrastructure using Azure CLI
# Usage:
#   .\deploy.ps1 -ResourceGroup <rg> -SubscriptionId <sub> [-Location <loc>] [-Environment <env>] [-PostgresPassword <pwd>]
#
# Prerequisites:
#   - Azure CLI installed and authenticated (az login)
#   - Bicep CLI installed (az bicep install)

param(
    [Parameter(Mandatory = $true)]
    [string]$ResourceGroup,

    [Parameter(Mandatory = $true)]
    [string]$SubscriptionId,

    [Parameter(Mandatory = $false)]
    [string]$Location = "eastus",

    [Parameter(Mandatory = $false)]
    [ValidateSet("dev", "staging", "prod")]
    [string]$Environment = "dev",

    [Parameter(Mandatory = $false)]
    [string]$PostgresPassword = ""
)

$ErrorActionPreference = "Stop"
$ScriptDir = $PSScriptRoot
$DeploymentName = "assetmgr-infra-$(Get-Date -Format 'yyyyMMddHHmmss')"

# ─── Prompt for password if not supplied ─────────────────────────────────────
if ([string]::IsNullOrEmpty($PostgresPassword)) {
    $securePassword = Read-Host -Prompt "Enter PostgreSQL admin password" -AsSecureString
    $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $PostgresPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($BSTR)
    [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR)
}

# ─── Pre-flight ───────────────────────────────────────────────────────────────
Write-Host "==> Setting active subscription: $SubscriptionId"
az account set --subscription $SubscriptionId

Write-Host "==> Ensuring resource group '$ResourceGroup' exists in '$Location'"
az group create `
    --name $ResourceGroup `
    --location $Location `
    --output none

# ─── Deploy ───────────────────────────────────────────────────────────────────
Write-Host "==> Deploying infrastructure (deployment: $DeploymentName)"
az deployment group create `
    --name $DeploymentName `
    --resource-group $ResourceGroup `
    --template-file "$ScriptDir\main.bicep" `
    --parameters "$ScriptDir\parameters.json" `
    --parameters `
        location="$Location" `
        environment="$Environment" `
        postgresAdminPassword="$PostgresPassword" `
    --output table

Write-Host "==> Deployment complete. Retrieving outputs..."
az deployment group show `
    --name $DeploymentName `
    --resource-group $ResourceGroup `
    --query properties.outputs `
    --output json
