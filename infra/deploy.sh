#!/usr/bin/env bash
# Asset Manager - Azure Infrastructure Deployment Script (Linux/macOS)
# Usage: ./deploy.sh <resource-group> <location> [environment]
#
# Prerequisites:
#   - Azure CLI installed and logged in (az login)
#   - Sufficient permissions to create resources and role assignments
#
# Example:
#   ./deploy.sh rg-assetmgr-dev eastus dev

set -euo pipefail

RESOURCE_GROUP="${1:?ERROR: Resource group name is required. Usage: $0 <resource-group> <location> [environment]}"
LOCATION="${2:?ERROR: Location is required. Usage: $0 <resource-group> <location> [environment]}"
ENVIRONMENT="${3:-dev}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "============================================"
echo "  Asset Manager - Azure Infrastructure"
echo "============================================"
echo "  Resource Group : $RESOURCE_GROUP"
echo "  Location       : $LOCATION"
echo "  Environment    : $ENVIRONMENT"
echo "============================================"
echo ""

# Verify Azure CLI is logged in
if ! az account show --output none 2>/dev/null; then
  echo "ERROR: Not logged in to Azure CLI. Run 'az login' first."
  exit 1
fi

# Prompt for PostgreSQL admin password (not echoed to terminal)
read -rsp "Enter PostgreSQL admin password: " POSTGRES_PASSWORD
echo ""

if [[ -z "$POSTGRES_PASSWORD" ]]; then
  echo "ERROR: PostgreSQL admin password cannot be empty."
  exit 1
fi

# Create resource group if it doesn't exist
echo "Creating resource group '$RESOURCE_GROUP' in '$LOCATION'..."
az group create \
  --name "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --output none

echo "Deploying infrastructure (this may take 5-10 minutes)..."
az deployment group create \
  --resource-group "$RESOURCE_GROUP" \
  --template-file "$SCRIPT_DIR/main.bicep" \
  --parameters "$SCRIPT_DIR/parameters.json" \
  --parameters environmentName="$ENVIRONMENT" \
               location="$LOCATION" \
               postgresAdminPassword="$POSTGRES_PASSWORD" \
  --output table

echo ""
echo "Deployment complete! Fetching outputs..."
az deployment group show \
  --resource-group "$RESOURCE_GROUP" \
  --name "main" \
  --query "properties.outputs" \
  --output json
