#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Deploy Azure infrastructure for asset-manager-showpune.

.DESCRIPTION
  Provisions all Azure resources (Storage Account, Service Bus, PostgreSQL,
  Managed Identity) using Bicep templates and Azure CLI.

.EXAMPLE
  .\deploy.ps1 -ResourceGroupName "rg-asset-manager" -AdministratorLoginPassword (ConvertTo-SecureString "P@ssw0rd!" -AsPlainText -Force)
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$ResourceGroupName,

    [Parameter(Mandatory = $false)]
    [string]$Location = "eastus2",

    [Parameter(Mandatory = $false)]
    [string]$EnvironmentName = "dev",

    [Parameter(Mandatory = $false)]
    [string]$AdministratorLogin = "pgadmin",

    [Parameter(Mandatory = $true)]
    [SecureString]$AdministratorLoginPassword
)

$ErrorActionPreference = "Stop"

$ScriptDir = $PSScriptRoot

Write-Host "=== Azure Infrastructure Deployment ===" -ForegroundColor Cyan
Write-Host "Resource Group : $ResourceGroupName"
Write-Host "Location       : $Location"
Write-Host "Environment    : $EnvironmentName"
Write-Host ""

# Verify Azure CLI
Write-Host "[Pre-check] Verifying Azure CLI..." -ForegroundColor Yellow
$cliVersion = az --version 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "Azure CLI is not installed or not in PATH. Install from https://aka.ms/installazurecli"
    exit 1
}
Write-Host "Azure CLI found." -ForegroundColor Green

# Convert SecureString to plain text for CLI parameter passing
$BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($AdministratorLoginPassword)
$PlainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
[System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR)

# Step 1: Create resource group
Write-Host ""
Write-Host "[1/3] Creating resource group '$ResourceGroupName' in '$Location'..." -ForegroundColor Yellow
az group create --name $ResourceGroupName --location $Location --output none
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to create resource group '$ResourceGroupName'."
    exit 1
}
Write-Host "Resource group ready." -ForegroundColor Green

# Step 2: Deploy Bicep template
Write-Host ""
Write-Host "[2/3] Deploying Bicep template..." -ForegroundColor Yellow
$DeploymentName = "asset-manager-deploy-$(Get-Date -Format 'yyyyMMddHHmmss')"

az deployment group create `
    --name $DeploymentName `
    --resource-group $ResourceGroupName `
    --template-file "$ScriptDir/main.bicep" `
    --parameters "$ScriptDir/main.parameters.json" `
    --parameters "location=$Location" `
    --parameters "environmentName=$EnvironmentName" `
    --parameters "administratorLogin=$AdministratorLogin" `
    --parameters "administratorLoginPassword=$PlainPassword" `
    --output none

if ($LASTEXITCODE -ne 0) {
    Write-Error "Bicep deployment failed. Check the Azure portal or run with --debug for details."
    exit 1
}
Write-Host "Bicep deployment succeeded." -ForegroundColor Green

# Step 3: Retrieve deployment outputs
Write-Host ""
Write-Host "[3/3] Retrieving deployment outputs..." -ForegroundColor Yellow

$StorageAccountName  = az deployment group show --resource-group $ResourceGroupName --name $DeploymentName --query "properties.outputs.storageAccountName.value" -o tsv
$ServiceBusEndpoint  = az deployment group show --resource-group $ResourceGroupName --name $DeploymentName --query "properties.outputs.serviceBusEndpoint.value" -o tsv
$ServiceBusNamespace = az deployment group show --resource-group $ResourceGroupName --name $DeploymentName --query "properties.outputs.serviceBusNamespace.value" -o tsv
$PostgresFqdn        = az deployment group show --resource-group $ResourceGroupName --name $DeploymentName --query "properties.outputs.postgresFqdn.value" -o tsv
$DbName              = az deployment group show --resource-group $ResourceGroupName --name $DeploymentName --query "properties.outputs.postgresDatabaseName.value" -o tsv
$MiClientId          = az deployment group show --resource-group $ResourceGroupName --name $DeploymentName --query "properties.outputs.managedIdentityClientId.value" -o tsv
$MiPrincipalId       = az deployment group show --resource-group $ResourceGroupName --name $DeploymentName --query "properties.outputs.managedIdentityPrincipalId.value" -o tsv

# Install Service Connector extension for future app-to-DB connectivity setup
az extension add --name serviceconnector-passwordless --upgrade --yes 2>&1 | Out-Null

Write-Host ""
Write-Host "=== Deployment Complete ===" -ForegroundColor Green
Write-Host ""
Write-Host "Resource outputs:" -ForegroundColor Cyan
Write-Host "  Storage Account Name      : $StorageAccountName"
Write-Host "  Service Bus Namespace     : $ServiceBusNamespace"
Write-Host "  Service Bus FQDN          : $ServiceBusEndpoint"
Write-Host "  PostgreSQL FQDN           : $PostgresFqdn"
Write-Host "  PostgreSQL Database       : $DbName"
Write-Host "  Managed Identity ClientId : $MiClientId"
Write-Host "  Managed Identity ObjectId : $MiPrincipalId"
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Update web/src/main/resources/application.properties:"
Write-Host "       azure.storage.account-name=$StorageAccountName"
Write-Host "       spring.cloud.azure.servicebus.namespace=$ServiceBusNamespace"
Write-Host "       spring.datasource.url=jdbc:postgresql://${PostgresFqdn}:5432/${DbName}"
Write-Host "  2. Assign the managed identity (clientId: $MiClientId) to your compute service (App Service / Container Apps)."
Write-Host "  3. When deploying to Azure Container Apps, run Service Connector to configure the PostgreSQL MI connection:"
Write-Host "       az containerapp connection create postgres-flexible --connection asset-db-conn \"
Write-Host "         --user-identity client-id=$MiClientId subs-id=<subscription-id> \"
Write-Host "         --source-id <containerapp-resource-id> --tg $ResourceGroupName \"
Write-Host "         --server $($PostgresFqdn.Split('.')[0]) --database $DbName --client-type springBoot -y"
