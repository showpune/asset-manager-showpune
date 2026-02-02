#!/bin/bash

# Script to set up Azure migration planning tools
# This script performs the following:
# 1. Installs GitHub Copilot CLI
# 2. Downloads and installs the appmod tool
# 3. Creates an Azure migration plan

set -e

echo "=========================================="
echo "Azure Migration Planning Setup"
echo "=========================================="
echo ""

# Step 1: Install GitHub Copilot CLI
echo "Step 1: Installing GitHub Copilot CLI..."
echo "Running: curl -fsSL https://gh.io/copilot-install | bash"
curl -fsSL https://gh.io/copilot-install | bash
echo "GitHub Copilot CLI installed successfully."
echo ""

# Step 2: Download and install appmod tool
echo "Step 2: Downloading appmod tool..."
APPMOD_URL="https://aka.ms/appmod_linux-x64.tar.gz"
APPMOD_DIR="$HOME/.local/share/appmod"
APPMOD_TAR="/tmp/appmod_linux-x64.tar.gz"

# Create temporary directory
mkdir -p "$APPMOD_DIR"

# Download the appmod tool
echo "Downloading from $APPMOD_URL..."
curl -L -o "$APPMOD_TAR" "$APPMOD_URL"

# Extract the tool
echo "Extracting appmod tool..."
tar -xzf "$APPMOD_TAR" -C "$APPMOD_DIR"

# Find the appmod binary and add to PATH
APPMOD_BIN=$(find "$APPMOD_DIR" -name "appmod" -type f)
if [ -z "$APPMOD_BIN" ]; then
    echo "Error: Could not find appmod binary in the extracted files"
    exit 1
fi

# Make it executable
chmod +x "$APPMOD_BIN"

# Add to PATH for current session
export PATH="$(dirname "$APPMOD_BIN"):$PATH"

echo "appmod tool installed successfully."
echo "Location: $APPMOD_BIN"
echo ""

# Step 3: Run appmod plan creation
echo "Step 3: Creating Azure migration plan..."
echo "Running: appmod plan create 'Create a plan to migrate the project to Azure. I don't want to upgrade my Java this time'"
"$APPMOD_BIN" plan create "Create a plan to migrate the project to Azure. I don't want to upgrade my Java this time"

echo ""
echo "=========================================="
echo "Setup complete!"
echo "=========================================="
echo ""
echo "Note: The appmod tool is located at: $APPMOD_BIN"
echo "To use it in other terminal sessions, add it to your PATH:"
echo "  export PATH=\"$(dirname "$APPMOD_BIN"):\$PATH\""
