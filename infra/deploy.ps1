#Requires -Version 5.1
<#
.SYNOPSIS
    Deploy assets-manager Azure infrastructure using Azure CLI.

.DESCRIPTION
    Provisions Azure Blob Storage, Azure Service Bus, Azure Database for
    PostgreSQL Flexible Server, and User-Assigned Managed Identity for the
    assets-manager application.

.PARAMETER ResourceGroup
    Name of the Azure resource group (required).

.PARAMETER Location
    Azure region for all resources. Default: eastus.

.PARAMETER Environment
    Deployment environment: dev, staging, or prod. Default: dev.

.PARAMETER PostgresPassword
    PostgreSQL administrator password (required unless -WhatIf is set).

.PARAMETER AppName
    Application name prefix for resource naming. Default: assets-manager.

.PARAMETER WhatIf
    Preview changes without deploying.

.EXAMPLE
    .\infra\deploy.ps1 -ResourceGroup "rg-assets-manager-dev" -PostgresPassword "P@ssw0rd!"

.EXAMPLE
    .\infra\deploy.ps1 -ResourceGroup "rg-assets-manager-dev" -WhatIf
#>
[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Mandatory = $true)]
    [string]$ResourceGroup,

    [string]$Location = 'eastus',

    [ValidateSet('dev', 'staging', 'prod')]
    [string]$Environment = 'dev',

    [string]$AppName = 'assets-manager',

    [SecureString]$PostgresPassword,

    [switch]$WhatIf
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$DeploymentName = "assets-manager-$(Get-Date -Format 'yyyyMMddHHmmss')"

# ── Pre-flight ────────────────────────────────────────────────────────────────
Write-Host "==> Checking Azure CLI login..." -ForegroundColor Cyan
az account show --output none
if ($LASTEXITCODE -ne 0) { throw "Not logged in to Azure CLI. Run 'az login' first." }

Write-Host "==> Installing/updating Bicep CLI..." -ForegroundColor Cyan
az bicep install

Write-Host "==> Validating Bicep templates..." -ForegroundColor Cyan
az bicep build --file "$ScriptDir\main.bicep" --stdout | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Bicep validation failed." }
Write-Host "    Templates are valid." -ForegroundColor Green

# ── Resolve password ──────────────────────────────────────────────────────────
$PlainPassword = ''
if (-not $WhatIf) {
    if (-not $PostgresPassword) {
        $PostgresPassword = Read-Host -AsSecureString -Prompt "PostgreSQL admin password"
    }
    $PlainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($PostgresPassword)
    )
}

# ── Resource group ────────────────────────────────────────────────────────────
Write-Host "==> Ensuring resource group '$ResourceGroup' exists in '$Location'..." -ForegroundColor Cyan
az group create --name $ResourceGroup --location $Location --output none

# ── Deploy ────────────────────────────────────────────────────────────────────
$CommonParams = @(
    '--name',         $DeploymentName
    '--resource-group', $ResourceGroup
    '--template-file', "$ScriptDir\main.bicep"
    '--parameters',   "$ScriptDir\parameters.json"
    '--parameters',   "location=$Location", "appName=$AppName", "environment=$Environment"
)

if ($WhatIf) {
    Write-Host "==> Running What-If analysis (no changes will be made)..." -ForegroundColor Yellow
    az deployment group what-if @CommonParams
} else {
    $CommonParams += @('--parameters', "postgresAdminPassword=$PlainPassword")
    Write-Host "==> Deploying infrastructure (deployment: $DeploymentName)..." -ForegroundColor Cyan
    $OutputsJson = az deployment group create @CommonParams --query "properties.outputs" --output json
    if ($LASTEXITCODE -ne 0) { throw "Deployment failed." }

    $Outputs = $OutputsJson | ConvertFrom-Json
    Write-Host ""
    Write-Host "==> Deployment complete. Infrastructure outputs:" -ForegroundColor Green
    $Outputs.PSObject.Properties | ForEach-Object {
        Write-Host ("  {0}: {1}" -f $_.Name, $_.Value.value)
    }
}
