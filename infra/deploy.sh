#!/usr/bin/env bash
# Deploy Azure infrastructure for asset-manager-showpune
# Usage: ./deploy.sh -g <resource-group> [-l <location>] [-e <environment>] -u <admin-login> -p <admin-password>
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

usage() {
  echo "Usage: $0 -g <resource-group> [-l <location>] [-e <environment>] -u <admin-login> -p <admin-password>"
  echo "  -g  Resource group name (required)"
  echo "  -l  Azure region (default: eastus)"
  echo "  -e  Environment name (default: dev)"
  echo "  -u  PostgreSQL admin login (default: pgadmin)"
  echo "  -p  PostgreSQL admin password (required)"
  exit 1
}

RESOURCE_GROUP=""
LOCATION="eastus2"
ENVIRONMENT_NAME="dev"
ADMIN_LOGIN="pgadmin"
ADMIN_PASSWORD=""

while getopts "g:l:e:u:p:" opt; do
  case $opt in
    g) RESOURCE_GROUP="$OPTARG" ;;
    l) LOCATION="$OPTARG" ;;
    e) ENVIRONMENT_NAME="$OPTARG" ;;
    u) ADMIN_LOGIN="$OPTARG" ;;
    p) ADMIN_PASSWORD="$OPTARG" ;;
    *) usage ;;
  esac
done

if [[ -z "$RESOURCE_GROUP" || -z "$ADMIN_PASSWORD" ]]; then
  usage
fi

echo "=== Azure Infrastructure Deployment ==="
echo "Resource Group : $RESOURCE_GROUP"
echo "Location       : $LOCATION"
echo "Environment    : $ENVIRONMENT_NAME"
echo ""

# Verify Azure CLI
echo "[Pre-check] Verifying Azure CLI..."
az --version > /dev/null 2>&1 || { echo "ERROR: Azure CLI is not installed. Install from https://aka.ms/installazurecli"; exit 1; }
echo "Azure CLI found."

# Step 1: Create resource group
echo ""
echo "[1/3] Creating resource group '$RESOURCE_GROUP' in '$LOCATION'..."
az group create --name "$RESOURCE_GROUP" --location "$LOCATION" --output none
echo "Resource group ready."

# Step 2: Deploy Bicep template
echo ""
echo "[2/3] Deploying Bicep template..."
DEPLOYMENT_NAME="asset-manager-deploy-$(date +%Y%m%d%H%M%S)"

az deployment group create \
  --name "$DEPLOYMENT_NAME" \
  --resource-group "$RESOURCE_GROUP" \
  --template-file "$SCRIPT_DIR/main.bicep" \
  --parameters "$SCRIPT_DIR/main.parameters.json" \
  --parameters "location=$LOCATION" \
  --parameters "environmentName=$ENVIRONMENT_NAME" \
  --parameters "administratorLogin=$ADMIN_LOGIN" \
  --parameters "administratorLoginPassword=$ADMIN_PASSWORD" \
  --output none

echo "Bicep deployment succeeded."

# Step 3: Retrieve deployment outputs
echo ""
echo "[3/3] Retrieving deployment outputs..."

STORAGE_ACCOUNT=$(az deployment group show -g "$RESOURCE_GROUP" -n "$DEPLOYMENT_NAME" --query "properties.outputs.storageAccountName.value" -o tsv)
SERVICE_BUS_NS=$(az deployment group show -g "$RESOURCE_GROUP" -n "$DEPLOYMENT_NAME" --query "properties.outputs.serviceBusNamespace.value" -o tsv)
SERVICE_BUS_EP=$(az deployment group show -g "$RESOURCE_GROUP" -n "$DEPLOYMENT_NAME" --query "properties.outputs.serviceBusEndpoint.value" -o tsv)
POSTGRES_FQDN=$(az deployment group show -g "$RESOURCE_GROUP" -n "$DEPLOYMENT_NAME" --query "properties.outputs.postgresFqdn.value" -o tsv)
DB_NAME=$(az deployment group show -g "$RESOURCE_GROUP" -n "$DEPLOYMENT_NAME" --query "properties.outputs.postgresDatabaseName.value" -o tsv)
MI_CLIENT_ID=$(az deployment group show -g "$RESOURCE_GROUP" -n "$DEPLOYMENT_NAME" --query "properties.outputs.managedIdentityClientId.value" -o tsv)
MI_PRINCIPAL_ID=$(az deployment group show -g "$RESOURCE_GROUP" -n "$DEPLOYMENT_NAME" --query "properties.outputs.managedIdentityPrincipalId.value" -o tsv)

# Install Service Connector extension for future app-to-DB connectivity setup
az extension add --name serviceconnector-passwordless --upgrade --yes 2>&1 || true

echo ""
echo "=== Deployment Complete ==="
echo ""
echo "Resource outputs:"
echo "  Storage Account Name      : $STORAGE_ACCOUNT"
echo "  Service Bus Namespace     : $SERVICE_BUS_NS"
echo "  Service Bus FQDN          : $SERVICE_BUS_EP"
echo "  PostgreSQL FQDN           : $POSTGRES_FQDN"
echo "  PostgreSQL Database       : $DB_NAME"
echo "  Managed Identity ClientId : $MI_CLIENT_ID"
echo "  Managed Identity ObjectId : $MI_PRINCIPAL_ID"
echo ""
echo "Next steps:"
echo "  1. Update web/src/main/resources/application.properties:"
echo "       azure.storage.account-name=$STORAGE_ACCOUNT"
echo "       spring.cloud.azure.servicebus.namespace=$SERVICE_BUS_NS"
echo "       spring.datasource.url=jdbc:postgresql://$POSTGRES_FQDN:5432/$DB_NAME"
echo "  2. Assign the managed identity (clientId: $MI_CLIENT_ID) to your compute service."
echo "  3. When deploying to Azure Container Apps, run Service Connector:"
echo "       az containerapp connection create postgres-flexible --connection asset-db-conn \\"
echo "         --user-identity client-id=$MI_CLIENT_ID subs-id=<subscription-id> \\"
echo "         --source-id <containerapp-resource-id> --tg $RESOURCE_GROUP \\"
echo "         --server ${POSTGRES_FQDN%%.*} --database $DB_NAME --client-type springBoot -y"
