#!/bin/bash
# Script to download and setup the AppMod CLI tool

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
APPMOD_URL="https://appmodcli.blob.core.windows.net/privaterelease/appmod_linux-x64.tar.gz?sp=r&st=2026-02-02T08:31:30Z&se=2026-02-28T16:46:30Z&spr=https&sv=2024-11-04&sr=b&sig=KJLpTrRs83a9MfkoIzpmDYKsYC%2Bvqgt9uNGp39Hbzxs%3D"
APPMOD_TAR="$PROJECT_ROOT/appmod_linux-x64.tar.gz"
APPMOD_BIN="$PROJECT_ROOT/appmod"

echo "================================================"
echo "AppMod CLI Tool Setup"
echo "================================================"
echo ""

# Check if appmod is already installed
if [ -f "$APPMOD_BIN" ]; then
    echo "✓ AppMod CLI tool is already installed at: $APPMOD_BIN"
    echo ""
    "$APPMOD_BIN" --version 2>/dev/null || echo "Note: Unable to verify version"
    exit 0
fi

echo "Downloading AppMod CLI tool..."
echo "URL: $APPMOD_URL"
echo ""

# Download the tar.gz file
if command -v curl &> /dev/null; then
    curl -L "$APPMOD_URL" -o "$APPMOD_TAR"
elif command -v wget &> /dev/null; then
    wget "$APPMOD_URL" -O "$APPMOD_TAR"
else
    echo "Error: Neither curl nor wget is available. Please install one of them."
    exit 1
fi

# Check if download was successful
if [ ! -f "$APPMOD_TAR" ] || [ ! -s "$APPMOD_TAR" ]; then
    echo "Error: Download failed or file is empty"
    exit 1
fi

echo "✓ Download completed"
echo ""

# Extract the tar.gz file
echo "Extracting AppMod CLI tool..."
tar -xzf "$APPMOD_TAR" -C "$PROJECT_ROOT"

# Check if extraction was successful
if [ ! -f "$APPMOD_BIN" ]; then
    echo "Error: Extraction failed. 'appmod' binary not found."
    exit 1
fi

# Make the binary executable
chmod +x "$APPMOD_BIN"

echo "✓ Extraction completed"
echo ""

# Verify installation
echo "Verifying installation..."
if "$APPMOD_BIN" --version 2>/dev/null; then
    echo ""
    echo "================================================"
    echo "✓ AppMod CLI tool successfully installed!"
    echo "================================================"
    echo ""
    echo "Binary location: $APPMOD_BIN"
    echo ""
    echo "You can now run migration planning with:"
    echo "  ./scripts/run-migration-plan.sh"
else
    echo "Warning: Installation completed but unable to verify version"
fi
