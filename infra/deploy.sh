#!/usr/bin/env bash
# deploy.sh – Deploy Asset Manager Azure infrastructure using Azure CLI
# Usage:
#   ./infra/deploy.sh [--resource-group <rg>] [--location <region>] [--env <dev|test|prod>]
#
# Prerequisites:
#   - Azure CLI installed and logged in (az login)
#   - POSTGRES_ADMIN_PASSWORD environment variable set, or entered interactively

set -euo pipefail

# ── Defaults ────────────────────────────────────────────────────────────────
RESOURCE_GROUP="${RESOURCE_GROUP:-asset-manager-rg}"
LOCATION="${LOCATION:-eastus}"
ENVIRONMENT_NAME="${ENVIRONMENT_NAME:-dev}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ── Argument parsing ─────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --resource-group|-g) RESOURCE_GROUP="$2"; shift 2 ;;
    --location|-l)       LOCATION="$2";       shift 2 ;;
    --env|-e)            ENVIRONMENT_NAME="$2"; shift 2 ;;
    *) echo "Unknown argument: $1"; exit 1 ;;
  esac
done

# ── PostgreSQL password ───────────────────────────────────────────────────────
if [[ -z "${POSTGRES_ADMIN_PASSWORD:-}" ]]; then
  echo "POSTGRES_ADMIN_PASSWORD is not set."
  read -rsp "Enter PostgreSQL admin password: " POSTGRES_ADMIN_PASSWORD
  echo
fi

echo "============================================================"
echo " Asset Manager – Azure Infrastructure Deployment"
echo "============================================================"
echo " Resource Group : $RESOURCE_GROUP"
echo " Location       : $LOCATION"
echo " Environment    : $ENVIRONMENT_NAME"
echo "============================================================"

# ── Ensure resource group exists ─────────────────────────────────────────────
echo "[1/3] Ensuring resource group '$RESOURCE_GROUP' exists..."
az group create \
  --name "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --output none

# ── Validate the template ─────────────────────────────────────────────────────
echo "[2/3] Validating Bicep template..."
az deployment group validate \
  --resource-group "$RESOURCE_GROUP" \
  --template-file "$SCRIPT_DIR/main.bicep" \
  --parameters "$SCRIPT_DIR/parameters.json" \
  --parameters \
    location="$LOCATION" \
    environmentName="$ENVIRONMENT_NAME" \
    postgresAdminPassword="$POSTGRES_ADMIN_PASSWORD" \
  --output none

# ── Deploy ───────────────────────────────────────────────────────────────────
echo "[3/3] Deploying infrastructure..."
DEPLOY_OUTPUT=$(az deployment group create \
  --resource-group "$RESOURCE_GROUP" \
  --template-file "$SCRIPT_DIR/main.bicep" \
  --parameters "$SCRIPT_DIR/parameters.json" \
  --parameters \
    location="$LOCATION" \
    environmentName="$ENVIRONMENT_NAME" \
    postgresAdminPassword="$POSTGRES_ADMIN_PASSWORD" \
  --output json)

# ── Print outputs ─────────────────────────────────────────────────────────────
echo ""
echo "============================================================"
echo " Deployment complete – infrastructure outputs:"
echo "============================================================"
echo "$DEPLOY_OUTPUT" | \
  python3 -c "
import sys, json
outputs = json.load(sys.stdin).get('properties', {}).get('outputs', {})
for k, v in outputs.items():
    print(f'  {k}: {v[\"value\"]}')
"
echo "============================================================"
