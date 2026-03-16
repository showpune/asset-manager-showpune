#!/usr/bin/env bash
# deploy.sh - Deploy Asset Manager infrastructure to Azure using Azure CLI
# Usage: ./deploy.sh [OPTIONS]
#
# Options:
#   -g, --resource-group    Resource group name (required)
#   -l, --location          Azure region (default: eastus)
#   -e, --environment       Environment name: dev|staging|prod (default: dev)
#   -p, --postgres-password PostgreSQL admin password (required)
#   -n, --app-name          Application name (default: asset-manager)
#       --what-if           Perform a what-if analysis only (no deployment)
#   -h, --help              Show this help message

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Defaults
LOCATION="eastus"
ENVIRONMENT="dev"
APP_NAME="asset-manager"
WHAT_IF=false
RESOURCE_GROUP=""
POSTGRES_PASSWORD=""

usage() {
  grep '^#' "$0" | sed 's/^# \{0,1\}//'
  exit 0
}

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    -g|--resource-group)   RESOURCE_GROUP="$2";    shift 2 ;;
    -l|--location)         LOCATION="$2";          shift 2 ;;
    -e|--environment)      ENVIRONMENT="$2";       shift 2 ;;
    -p|--postgres-password) POSTGRES_PASSWORD="$2"; shift 2 ;;
    -n|--app-name)         APP_NAME="$2";          shift 2 ;;
    --what-if)             WHAT_IF=true;           shift   ;;
    -h|--help)             usage ;;
    *) echo "Unknown option: $1"; usage ;;
  esac
done

# Validate required parameters
if [[ -z "$RESOURCE_GROUP" ]]; then
  echo "ERROR: --resource-group is required." >&2
  exit 1
fi

if [[ -z "$POSTGRES_PASSWORD" ]] && [[ "$WHAT_IF" == "false" ]]; then
  echo "ERROR: --postgres-password is required for deployment." >&2
  exit 1
fi

# Ensure logged in
if ! az account show &>/dev/null; then
  echo "Not logged in to Azure. Running 'az login'..."
  az login
fi

echo "=== Asset Manager Infrastructure Deployment ==="
echo "Resource Group : $RESOURCE_GROUP"
echo "Location       : $LOCATION"
echo "Environment    : $ENVIRONMENT"
echo "App Name       : $APP_NAME"
echo ""

# Create resource group if it does not exist
if ! az group show --name "$RESOURCE_GROUP" &>/dev/null; then
  echo "Creating resource group '$RESOURCE_GROUP' in '$LOCATION'..."
  az group create \
    --name "$RESOURCE_GROUP" \
    --location "$LOCATION" \
    --tags "environment=$ENVIRONMENT" "application=$APP_NAME" "managedBy=bicep"
fi

DEPLOYMENT_NAME="asset-manager-infra-$(date +%Y%m%d%H%M%S)"

COMMON_ARGS=(
  --resource-group "$RESOURCE_GROUP"
  --template-file "$SCRIPT_DIR/main.bicep"
  --parameters "$SCRIPT_DIR/parameters.json"
  --parameters
    location="$LOCATION"
    environmentName="$ENVIRONMENT"
    appName="$APP_NAME"
)

if [[ "$WHAT_IF" == "true" ]]; then
  echo "Running what-if analysis..."
  az deployment group what-if \
    "${COMMON_ARGS[@]}" \
    --parameters postgresAdminPassword="$POSTGRES_PASSWORD"
else
  echo "Deploying infrastructure (deployment: $DEPLOYMENT_NAME)..."
  az deployment group create \
    --name "$DEPLOYMENT_NAME" \
    "${COMMON_ARGS[@]}" \
    --parameters postgresAdminPassword="$POSTGRES_PASSWORD" \
    --output json | tee /tmp/deployment-output.json

  echo ""
  echo "=== Deployment Outputs ==="
  az deployment group show \
    --resource-group "$RESOURCE_GROUP" \
    --name "$DEPLOYMENT_NAME" \
    --query "properties.outputs" \
    --output table

  echo ""
  echo "Deployment '$DEPLOYMENT_NAME' completed successfully."
fi
