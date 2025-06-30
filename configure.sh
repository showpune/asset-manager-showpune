#!/bin/bash

# Asset Manager Configuration Script
# Use this script to easily switch between storage providers

show_help() {
    echo "Asset Manager Configuration Script"
    echo ""
    echo "Usage: $0 [OPTION]"
    echo ""
    echo "Options:"
    echo "  --azure     Configure for Azure Blob Storage and Service Bus"
    echo "  --s3        Configure for AWS S3 and RabbitMQ"
    echo "  --dev       Configure for local file system and RabbitMQ (development)"
    echo "  --status    Show current configuration"
    echo "  --help      Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 --azure    # Switch to Azure configuration"
    echo "  $0 --s3       # Switch to AWS S3 configuration"
    echo "  $0 --dev      # Switch to development configuration"
}

set_profile() {
    local profile=$1
    echo "Setting Spring Boot profile to: $profile"
    
    # Update web application
    if [ -f "web/src/main/resources/application.properties" ]; then
        if grep -q "spring.profiles.active" web/src/main/resources/application.properties; then
            sed -i "s/spring.profiles.active=.*/spring.profiles.active=$profile/" web/src/main/resources/application.properties
        else
            echo "spring.profiles.active=$profile" >> web/src/main/resources/application.properties
        fi
    fi
    
    # Update worker application
    if [ -f "worker/src/main/resources/application.properties" ]; then
        if grep -q "spring.profiles.active" worker/src/main/resources/application.properties; then
            sed -i "s/spring.profiles.active=.*/spring.profiles.active=$profile/" worker/src/main/resources/application.properties
        else
            echo "spring.profiles.active=$profile" >> worker/src/main/resources/application.properties
        fi
    fi
    
    echo "Configuration updated successfully!"
    echo ""
    case $profile in
        "azure")
            echo "Now configured for Azure Blob Storage and Service Bus"
            echo "Make sure to set these environment variables:"
            echo "  export AZURE_CLIENT_ID=your-managed-identity-client-id"
            echo "  export SERVICE_BUS_NAMESPACE=your-servicebus-namespace.servicebus.windows.net"
            ;;
        "s3")
            echo "Now configured for AWS S3 and RabbitMQ"
            echo "Make sure to configure your AWS credentials in application-s3.properties"
            ;;
        "dev")
            echo "Now configured for local development (local file system + RabbitMQ)"
            echo "Use Docker to start RabbitMQ and PostgreSQL services"
            ;;
    esac
}

show_status() {
    echo "Current Configuration Status:"
    echo ""
    
    if [ -f "web/src/main/resources/application.properties" ]; then
        web_profile=$(grep "spring.profiles.active" web/src/main/resources/application.properties | cut -d'=' -f2 || echo "default")
        echo "Web module profile: $web_profile"
    fi
    
    if [ -f "worker/src/main/resources/application.properties" ]; then
        worker_profile=$(grep "spring.profiles.active" worker/src/main/resources/application.properties | cut -d'=' -f2 || echo "default")
        echo "Worker module profile: $worker_profile"
    fi
    
    echo ""
    echo "Available profiles:"
    echo "  - azure: Azure Blob Storage + Azure Service Bus"
    echo "  - s3: AWS S3 + RabbitMQ"
    echo "  - dev: Local file system + RabbitMQ (default)"
}

case "$1" in
    --azure)
        set_profile "azure"
        ;;
    --s3)
        set_profile "s3"
        ;;
    --dev)
        set_profile "dev"
        ;;
    --status)
        show_status
        ;;
    --help)
        show_help
        ;;
    *)
        echo "Error: Invalid option '$1'"
        echo ""
        show_help
        exit 1
        ;;
esac