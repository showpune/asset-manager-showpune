#!/usr/bin/env bash
# =============================================================================
# deploy.sh — Deploy assets-manager Azure infrastructure using Azure CLI
#
# Usage:
#   ./infra/deploy.sh [OPTIONS]
#
# Options:
#   -g, --resource-group    Resource group name (required)
#   -l, --location          Azure region              (default: eastus)
#   -e, --environment       Environment name          (default: dev)
#   -p, --postgres-password PostgreSQL admin password (required)
#   -n, --app-name          Application name prefix   (default: assets-manager)
#       --what-if           Preview changes without deploying
#
# Prerequisites:
#   - Azure CLI >= 2.50.0 installed and logged in (az login)
#   - Bicep CLI installed (az bicep install)
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ── Defaults ──────────────────────────────────────────────────────────────────
LOCATION="eastus"
ENVIRONMENT="dev"
APP_NAME="assets-manager"
POSTGRES_PASSWORD=""
WHAT_IF=false

# ── Argument parsing ──────────────────────────────────────────────────────────
usage() {
  grep '^#' "$0" | grep -v '#!/' | sed 's/^# \{0,\}//'
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -g|--resource-group)    RESOURCE_GROUP="$2"; shift 2 ;;
    -l|--location)          LOCATION="$2";        shift 2 ;;
    -e|--environment)       ENVIRONMENT="$2";     shift 2 ;;
    -p|--postgres-password) POSTGRES_PASSWORD="$2"; shift 2 ;;
    -n|--app-name)          APP_NAME="$2";        shift 2 ;;
    --what-if)              WHAT_IF=true;         shift   ;;
    -h|--help)              usage ;;
    *) echo "Unknown option: $1"; usage ;;
  esac
done

if [[ -z "${RESOURCE_GROUP:-}" ]]; then
  echo "ERROR: --resource-group is required." >&2
  usage
fi

if [[ -z "${POSTGRES_PASSWORD}" ]] && [[ "${WHAT_IF}" == "false" ]]; then
  read -r -s -p "PostgreSQL admin password: " POSTGRES_PASSWORD
  echo
fi

DEPLOYMENT_NAME="assets-manager-$(date +%Y%m%d%H%M%S)"

# ── Pre-flight checks ─────────────────────────────────────────────────────────
echo "==> Checking Azure CLI login..."
az account show --output none

echo "==> Installing/updating Bicep CLI..."
az bicep install

echo "==> Validating Bicep templates..."
az bicep build --file "${SCRIPT_DIR}/main.bicep" --stdout > /dev/null
echo "    Templates are valid."

# ── Resource group ────────────────────────────────────────────────────────────
echo "==> Ensuring resource group '${RESOURCE_GROUP}' exists in '${LOCATION}'..."
az group create \
  --name "${RESOURCE_GROUP}" \
  --location "${LOCATION}" \
  --output none

# ── Build deployment parameters ───────────────────────────────────────────────
EXTRA_PARAMS=(
  "location=${LOCATION}"
  "appName=${APP_NAME}"
  "environment=${ENVIRONMENT}"
)

if [[ -n "${POSTGRES_PASSWORD}" ]]; then
  EXTRA_PARAMS+=("postgresAdminPassword=${POSTGRES_PASSWORD}")
fi

# ── Deploy or What-If ─────────────────────────────────────────────────────────
if [[ "${WHAT_IF}" == "true" ]]; then
  echo "==> Running What-If analysis (no changes will be made)..."
  az deployment group what-if \
    --name "${DEPLOYMENT_NAME}" \
    --resource-group "${RESOURCE_GROUP}" \
    --template-file "${SCRIPT_DIR}/main.bicep" \
    --parameters "${SCRIPT_DIR}/parameters.json" \
    --parameters "${EXTRA_PARAMS[@]}"
else
  echo "==> Deploying infrastructure (deployment: ${DEPLOYMENT_NAME})..."
  OUTPUTS=$(az deployment group create \
    --name "${DEPLOYMENT_NAME}" \
    --resource-group "${RESOURCE_GROUP}" \
    --template-file "${SCRIPT_DIR}/main.bicep" \
    --parameters "${SCRIPT_DIR}/parameters.json" \
    --parameters "${EXTRA_PARAMS[@]}" \
    --query "properties.outputs" \
    --output json)

  echo ""
  echo "==> Deployment complete. Infrastructure outputs:"
  echo "${OUTPUTS}" | python3 -c "
import json, sys
outputs = json.load(sys.stdin)
for k, v in outputs.items():
    print(f'  {k}: {v[\"value\"]}')"
fi
