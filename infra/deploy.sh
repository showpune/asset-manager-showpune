#!/usr/bin/env bash
# deploy.sh – Deploy Asset Manager Kit Azure infrastructure using Azure CLI
# Usage:
#   ./deploy.sh -g <resource-group> -s <subscription-id> [-l <location>] [-e <environment>] [-p <postgres-password>]
#
# Prerequisites:
#   - Azure CLI installed and authenticated (az login)
#   - Bicep CLI installed (az bicep install)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ─── Defaults ─────────────────────────────────────────────────────────────────
LOCATION="eastus"
ENVIRONMENT="dev"
POSTGRES_PASSWORD=""
DEPLOYMENT_NAME="assetmgr-infra-$(date +%Y%m%d%H%M%S)"

# ─── Argument parsing ─────────────────────────────────────────────────────────
usage() {
  echo "Usage: $0 -g <resource-group> -s <subscription-id> [-l <location>] [-e <environment>] [-p <postgres-password>]"
  exit 1
}

while getopts "g:s:l:e:p:h" opt; do
  case "${opt}" in
    g) RESOURCE_GROUP="${OPTARG}" ;;
    s) SUBSCRIPTION_ID="${OPTARG}" ;;
    l) LOCATION="${OPTARG}" ;;
    e) ENVIRONMENT="${OPTARG}" ;;
    p) POSTGRES_PASSWORD="${OPTARG}" ;;
    h|*) usage ;;
  esac
done

if [[ -z "${RESOURCE_GROUP:-}" || -z "${SUBSCRIPTION_ID:-}" ]]; then
  echo "ERROR: -g <resource-group> and -s <subscription-id> are required."
  usage
fi

if [[ -z "${POSTGRES_PASSWORD}" ]]; then
  read -r -s -p "Enter PostgreSQL admin password: " POSTGRES_PASSWORD
  echo
fi

# ─── Pre-flight ───────────────────────────────────────────────────────────────
echo "==> Setting active subscription: ${SUBSCRIPTION_ID}"
az account set --subscription "${SUBSCRIPTION_ID}"

echo "==> Ensuring resource group '${RESOURCE_GROUP}' exists in '${LOCATION}'"
az group create \
  --name "${RESOURCE_GROUP}" \
  --location "${LOCATION}" \
  --output none

# ─── Deploy ───────────────────────────────────────────────────────────────────
echo "==> Deploying infrastructure (deployment: ${DEPLOYMENT_NAME})"
az deployment group create \
  --name "${DEPLOYMENT_NAME}" \
  --resource-group "${RESOURCE_GROUP}" \
  --template-file "${SCRIPT_DIR}/main.bicep" \
  --parameters "${SCRIPT_DIR}/parameters.json" \
  --parameters \
      location="${LOCATION}" \
      environment="${ENVIRONMENT}" \
      postgresAdminPassword="${POSTGRES_PASSWORD}" \
  --output table

echo "==> Deployment complete. Retrieving outputs..."
az deployment group show \
  --name "${DEPLOYMENT_NAME}" \
  --resource-group "${RESOURCE_GROUP}" \
  --query properties.outputs \
  --output json
