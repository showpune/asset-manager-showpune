#!/bin/bash

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/.."

# Create tools directory if it doesn't exist
TOOLS_DIR="$PROJECT_ROOT/tools"
mkdir -p "$TOOLS_DIR"

# Define the URL and target file
APPMOD_URL="https://aka.ms/appmod_linux-x64.tar.gz"
APPMOD_TAR="$TOOLS_DIR/appmod_linux-x64.tar.gz"
APPMOD_DIR="$TOOLS_DIR/appmod"

echo "Downloading appmod CLI tool from $APPMOD_URL..."

# Download the tool
if ! curl -L -o "$APPMOD_TAR" "$APPMOD_URL"; then
    echo "Error: Failed to download appmod tool"
    exit 1
fi

echo "Download completed successfully."

# Extract the tar.gz file
echo "Extracting appmod tool..."
mkdir -p "$APPMOD_DIR"
if ! tar -xzf "$APPMOD_TAR" -C "$APPMOD_DIR"; then
    echo "Error: Failed to extract appmod tool"
    exit 1
fi

echo "Extraction completed successfully."

# Find the appmod executable
APPMOD_EXECUTABLE=$(find "$APPMOD_DIR" -name "appmod" -type f | head -n 1)

if [ -z "$APPMOD_EXECUTABLE" ]; then
    echo "Error: Could not find appmod executable"
    exit 1
fi

echo "Found appmod executable at: $APPMOD_EXECUTABLE"

# Make it executable
chmod +x "$APPMOD_EXECUTABLE"

# Change to project root before running appmod
cd "$PROJECT_ROOT"

# Run the appmod command
echo "Running appmod plan create command..."
"$APPMOD_EXECUTABLE" plan create "Create a plan to migrate the project to Azure. I don't want to upgrade my Java this time"

echo "Script completed!"
