# Azure Migration Planning with AppMod CLI

This document describes how to use the AppMod CLI tool to create a migration plan for moving the Asset Manager application from AWS to Azure.

## Overview

The Asset Manager is a Java Spring Boot application that currently uses:
- AWS S3 for image storage (with password-based auth)
- RabbitMQ for message queuing (with password-based auth)  
- PostgreSQL for metadata storage (with password-based auth)

The migration goal is to move to Azure services:
- Azure Blob Storage (with managed identity auth)
- Azure Service Bus (with managed identity auth)
- Azure Database for PostgreSQL (with managed identity auth)

**Important:** The migration should be done without upgrading the Java version (currently Java 17).

## Prerequisites

- Linux, macOS, or Windows with WSL
- curl or wget installed
- Internet access to download the AppMod CLI tool

## Quick Start

### Step 1: Download and Setup AppMod CLI

Run the setup script for your platform:

**Linux/Mac:**
```bash
./scripts/setup-appmod.sh
```

**Windows:**
```batch
scripts\setup-appmod.cmd
```

The script will:
1. Download the AppMod CLI tool from Azure blob storage
2. Extract it to the project root directory
3. Make it executable
4. Verify the installation

### Step 2: Generate Migration Plan

Run the migration planning script:

**Linux/Mac:**
```bash
./scripts/run-migration-plan.sh
```

**Windows:**
```batch
scripts\run-migration-plan.cmd
```

This executes:
```
appmod -- plan create "Create a plan to migrate the project to Azure. I don't want to upgrade my Java this time"
```

## What the AppMod Tool Does

The AppMod CLI tool will:

1. **Analyze the codebase** - Scan Java source files to understand:
   - Dependencies and libraries used
   - AWS SDK usage patterns
   - Configuration files
   - Authentication mechanisms

2. **Generate migration recommendations** - Provide:
   - Azure service mappings (S3 → Blob Storage, RabbitMQ → Service Bus, etc.)
   - Code changes needed for Azure SDKs
   - Authentication migration (password → managed identity)
   - Configuration updates
   - Best practices for Azure deployment

3. **Create a migration plan** - Output includes:
   - Step-by-step migration tasks
   - Code refactoring suggestions
   - Infrastructure changes needed
   - Testing recommendations

## Expected Output

The migration plan will be saved in the `.appmod/` directory or displayed in the console. Look for:

- **Service Mappings**: How AWS services map to Azure equivalents
- **Code Changes**: Specific files and lines that need updating
- **Configuration Updates**: Changes to application.properties or application.yml
- **Dependencies**: Maven/Gradle dependency updates needed
- **Authentication Changes**: Migration from password-based to managed identity auth

## Manual Download (If Scripts Fail)

If the automated scripts fail due to network restrictions:

1. **Download manually:**
   - URL: https://appmodcli.blob.core.windows.net/privaterelease/appmod_linux-x64.tar.gz?sp=r&st=2026-02-02T08:31:30Z&se=2026-02-28T16:46:30Z&spr=https&sv=2024-11-04&sr=b&sig=KJLpTrRs83a9MfkoIzpmDYKsYC%2Bvqgt9uNGp39Hbzxs%3D
   - Save as: `appmod_linux-x64.tar.gz` in the project root

2. **Extract:**
   ```bash
   tar -xzf appmod_linux-x64.tar.gz
   ```

3. **Make executable:**
   ```bash
   chmod +x appmod
   ```

4. **Run the command:**
   ```bash
   ./appmod -- plan create "Create a plan to migrate the project to Azure. I don't want to upgrade my Java this time"
   ```

## Troubleshooting

### Network/Download Issues

If you see DNS resolution errors or timeouts:
- Check your internet connection
- Verify you can access Azure blob storage (corporate firewall may block it)
- Try downloading from a different network
- Use a VPN if your network blocks Azure domains

### Linux Binary on Windows

The downloaded tool is a Linux binary. On Windows:
- Use WSL (Windows Subsystem for Linux)
- Or run in a Linux VM/Docker container
- Or contact the tool provider for a Windows version

### Permission Issues

If you get "Permission denied" errors:
```bash
chmod +x appmod
chmod +x scripts/*.sh
```

## Next Steps After Migration Planning

Once you have the migration plan:

1. **Review the plan** - Understand all suggested changes
2. **Set up Azure resources** - Create Azure accounts and services
3. **Update dependencies** - Add Azure SDK dependencies to pom.xml
4. **Refactor code** - Implement the suggested code changes
5. **Update configuration** - Modify application properties
6. **Test locally** - Use Azure Storage Emulator, etc.
7. **Deploy to Azure** - Follow Azure deployment best practices

## Additional Resources

- [Azure Migration Guide](https://docs.microsoft.com/azure/architecture/migration/)
- [Azure SDK for Java](https://docs.microsoft.com/java/azure/)
- [Azure Identity Authentication](https://docs.microsoft.com/azure/developer/java/sdk/identity)
- Project README: [README.md](README.md)

## Files Added to Repository

- `scripts/setup-appmod.sh` - Linux/Mac setup script
- `scripts/setup-appmod.cmd` - Windows setup script
- `scripts/run-migration-plan.sh` - Linux/Mac migration planning script
- `scripts/run-migration-plan.cmd` - Windows migration planning script
- `.gitignore` - Updated to exclude AppMod CLI binaries

## Important Notes

- The AppMod CLI tool and downloaded archives are excluded from version control (see `.gitignore`)
- The tool download link has an expiration date (check the URL for the `se` parameter)
- Migration plans are project-specific and should be reviewed carefully before implementation
- Always test thoroughly in a development environment before production migration
