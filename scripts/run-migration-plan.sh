#!/bin/bash
# Script to run the AppMod migration planning command

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
APPMOD_BIN="$PROJECT_ROOT/appmod"

echo "================================================"
echo "AppMod Migration Planning"
echo "================================================"
echo ""

# Check if appmod is installed
if [ ! -f "$APPMOD_BIN" ]; then
    echo "Error: AppMod CLI tool is not installed."
    echo "Please run: ./scripts/setup-appmod.sh"
    exit 1
fi

# Make sure it's executable
if [ ! -x "$APPMOD_BIN" ]; then
    chmod +x "$APPMOD_BIN"
fi

echo "Project: Asset Manager (Java Spring Boot)"
echo "Migration Target: Azure"
echo ""
echo "Running migration plan creation..."
echo ""

# Change to project root to run appmod
cd "$PROJECT_ROOT"

# Run the appmod command with the specified plan
"$APPMOD_BIN" -- plan create "Create a plan to migrate the project to Azure. I don't want to upgrade my Java this time"

echo ""
echo "================================================"
echo "✓ Migration plan creation completed"
echo "================================================"
echo ""
echo "Check the output above for the generated migration plan."
echo "The plan should be saved in the .appmod directory or similar location."
