# Scripts Documentation

This directory contains utility scripts for the Asset Manager project.

## run-appmod.sh

This script downloads and runs the GitHub Copilot app modernization CLI tool (appmod) to create a migration plan for the project.

### Usage

```bash
./scripts/run-appmod.sh
```

### What it does

1. Downloads the appmod CLI tool from https://aka.ms/appmod_linux-x64.tar.gz
2. Extracts the tool to the `tools/` directory
3. Runs the command: `appmod plan create "Create a plan to migrate the project to Azure. I don't want to upgrade my Java this time"`
4. Creates a modernization plan in `.github/modernization/` directory
5. Creates a new branch `001-modernization-plan` for the migration work

### Prerequisites

- curl (for downloading)
- tar (for extracting)
- Linux x64 system

### Notes

- The `tools/` directory is automatically created and is gitignored
- The appmod tool will auto-detect the project language (Java in this case)
- A warning about GitHub Copilot CLI not being installed may appear but doesn't prevent the tool from functioning
- The tool creates a branch for the modernization work automatically

## Other Scripts

- **start.sh** - Starts the web and worker applications with PostgreSQL and RabbitMQ containers
- **stop.sh** - Stops all running services and containers
- **start.cmd** - Windows version of start.sh
- **stop.cmd** - Windows version of stop.sh
