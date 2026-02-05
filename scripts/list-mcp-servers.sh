#!/bin/bash

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/.."
MCP_CONFIG_FILE="$PROJECT_ROOT/.github/mcp-config.json"

echo "================================================"
echo "       MCP Servers Configuration List          "
echo "================================================"
echo ""

# Check if the config file exists
if [ ! -f "$MCP_CONFIG_FILE" ]; then
    echo "Error: MCP configuration file not found at $MCP_CONFIG_FILE"
    echo ""
    echo "================================================"
    exit 1
fi

# Check if jq is installed for pretty JSON parsing
if command -v jq &> /dev/null; then
    echo "MCP Servers found in .github/mcp-config.json:"
    echo ""
    
    # Parse and display each MCP server
    jq -r '.mcpServers | to_entries[] | "Server Name: \(.key)\n  Type: \(.value.type)\n  Command: \(.value.command)\n  Args: \(.value.args | join(" "))\n  Tools: \(.value.tools | join(", "))\n"' "$MCP_CONFIG_FILE"
else
    # Fallback to basic parsing if jq is not available
    echo "MCP Servers (raw JSON):"
    echo ""
    cat "$MCP_CONFIG_FILE"
    echo ""
    echo ""
    echo "Tip: Install 'jq' for better formatted output"
    echo "  - Ubuntu/Debian: sudo apt-get install jq"
    echo "  - macOS: brew install jq"
    echo "  - Red Hat/CentOS: sudo yum install jq"
fi

echo ""
echo "================================================"
