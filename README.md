# Asset Manager - Showpune

This is a Spring Boot-based Asset Manager application with web and worker modules.

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Docker (for PostgreSQL and RabbitMQ)

## Project Structure

```
asset-manager-showpune/
├── web/           # Web module (REST API, Controllers)
├── worker/        # Worker module (Background processing)
├── scripts/       # Utility scripts
└── .github/       # GitHub configuration including MCP servers
```

## Scripts

### Starting the Application

**Linux/Mac:**
```bash
bash scripts/start.sh
```

**Windows:**
```cmd
scripts\start.cmd
```

This will:
- Start PostgreSQL container
- Start RabbitMQ container
- Launch the web module on port 8080
- Launch the worker module on port 8081

### Stopping the Application

**Linux/Mac:**
```bash
bash scripts/stop.sh
```

**Windows:**
```cmd
scripts\stop.cmd
```

### Listing MCP Servers

To view all configured MCP (Model Context Protocol) servers:

**Linux/Mac:**
```bash
bash scripts/list-mcp-servers.sh
```

**Windows:**
```cmd
scripts\list-mcp-servers.cmd
```

This script reads the `.github/mcp-config.json` file and displays all configured MCP servers with their details including:
- Server name
- Server type
- Command and arguments
- Available tools

## Endpoints

- **Web Application**: http://localhost:8080
- **Worker Application**: http://localhost:8081
- **RabbitMQ Management Console**: http://localhost:15672 (credentials: guest/guest)

## MCP Servers

MCP (Model Context Protocol) servers are configured in `.github/mcp-config.json`. These servers provide additional capabilities for code modernization and other tasks. Use the `list-mcp-servers` script to view all configured servers.

## Building

To build the project:

```bash
./mvnw clean install
```

## License

See [LICENSE](LICENSE) file for details.
