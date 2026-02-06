# Migration Guide: RabbitMQ to Azure Service Bus

This guide provides detailed information about the migration from RabbitMQ with AMQP to Azure Service Bus for the Asset Manager Kit application.

---

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Architecture Changes](#architecture-changes)
4. [Configuration Changes](#configuration-changes)
5. [Code Changes](#code-changes)
6. [Azure Resource Setup](#azure-resource-setup)
7. [Testing](#testing)
8. [Deployment](#deployment)
9. [Troubleshooting](#troubleshooting)

---

## Overview

### What Changed
- **From**: RabbitMQ with AMQP protocol
- **To**: Azure Service Bus with JMS API
- **Authentication**: Managed Identity (passwordless)
- **Messaging Pattern**: Queue-based messaging maintained

### Why Azure Service Bus?
- Fully managed cloud service (no infrastructure to maintain)
- Built-in high availability and disaster recovery
- Managed identity support for secure authentication
- Enterprise-grade message broker with advanced features
- Seamless integration with Azure ecosystem

---

## Prerequisites

### Software Requirements
- Java 21 LTS (upgraded from Java 11)
- Spring Boot 3.4.0 (upgraded from 2.7.14)
- Maven 3.x
- Azure CLI (for resource setup)

### Azure Requirements
- Azure subscription
- Permissions to create Azure Service Bus resources
- Permissions to assign RBAC roles

---

## Architecture Changes

### Before Migration
```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│  Web App    │────────▶│  RabbitMQ    │────────▶│ Worker App  │
│  (Producer) │         │  (Broker)    │         │ (Consumer)  │
└─────────────┘         └──────────────┘         └─────────────┘
     |                                                   |
     └───────────────── PostgreSQL ─────────────────────┘
```

### After Migration
```
┌─────────────┐         ┌──────────────────┐         ┌─────────────┐
│  Web App    │────────▶│ Azure Service    │────────▶│ Worker App  │
│  (Producer) │         │ Bus (Managed)    │         │ (Consumer)  │
└─────────────┘         └──────────────────┘         └─────────────┘
     |                          ▲                          |
     |                          │                          |
     |                  Managed Identity                   |
     |                   (Passwordless)                    |
     |                                                     |
     └──────────────── PostgreSQL ────────────────────────┘
```

---

## Configuration Changes

### Web Module Configuration

**File**: `web/src/main/resources/application-azure.properties`

```properties
# Azure Service Bus Configuration
spring.cloud.azure.credential.managed-identity-enabled=true
spring.cloud.azure.credential.client-id=${AZURE_CLIENT_ID}
spring.cloud.azure.servicebus.entity-type=queue
spring.cloud.azure.servicebus.namespace=${SERVICE_BUS_NAMESPACE}
spring.cloud.azure.servicebus.pricing-tier=premium

# JMS Configuration for Service Bus
spring.jms.servicebus.pricing-tier=premium
spring.jms.servicebus.idle-timeout=60000
```

**Environment Variables**:
- `AZURE_CLIENT_ID`: Client ID for user-assigned managed identity (optional for system-assigned)
- `SERVICE_BUS_NAMESPACE`: Name of your Service Bus namespace (e.g., `mycompany-servicebus`)

### Worker Module Configuration

**File**: `worker/src/main/resources/application-azure.properties`

```properties
# Azure Service Bus Configuration
spring.cloud.azure.credential.managed-identity-enabled=true
spring.cloud.azure.credential.client-id=${AZURE_CLIENT_ID}
spring.cloud.azure.servicebus.entity-type=queue
spring.cloud.azure.servicebus.namespace=${SERVICE_BUS_NAMESPACE}
spring.cloud.azure.servicebus.pricing-tier=premium

# JMS Configuration for Service Bus
spring.jms.servicebus.pricing-tier=premium
spring.jms.servicebus.idle-timeout=60000
```

### Removed RabbitMQ Configuration
The following RabbitMQ properties have been removed:
- `spring.rabbitmq.host`
- `spring.rabbitmq.port`
- `spring.rabbitmq.username`
- `spring.rabbitmq.password`
- `spring.rabbitmq.virtual-host`
- `spring.rabbitmq.ssl.enabled`

---

## Code Changes

### Dependency Changes

**Removed**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

**Added**:
```xml
<dependency>
    <groupId>com.azure.spring</groupId>
    <artifactId>spring-cloud-azure-starter-servicebus-jms</artifactId>
</dependency>
```

### Service Bus Configuration Class

Both web and worker modules use ServiceBusConfig classes that configure JMS for Azure Service Bus:

**Key Features**:
- Uses `DefaultAzureCredential` for managed identity authentication
- Configures JMS connection factory for Service Bus
- Sets up message converters (Jackson for JSON)
- Maintains existing message patterns

**Example** (web/src/main/java/.../config/ServiceBusConfig.java):
```java
@Configuration
@Profile("azure")
public class ServiceBusConfig {
    
    @Bean
    public ConnectionFactory connectionFactory(ServiceBusJmsConnectionFactory factory) {
        // Uses managed identity automatically via Spring Cloud Azure
        return factory.createConnectionFactory();
    }
    
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setMessageConverter(jackson2JmsMessageConverter());
        return template;
    }
    
    @Bean
    public MessageConverter jackson2JmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTypeIdPropertyName("_type");
        return converter;
    }
}
```

### Message Sending (Web Module)

No code changes required for message sending! The existing JMS code works with Azure Service Bus:

```java
@Service
public class MessageService {
    
    @Autowired
    private JmsTemplate jmsTemplate;
    
    public void sendMessage(String queueName, Object message) {
        jmsTemplate.convertAndSend(queueName, message);
    }
}
```

### Message Receiving (Worker Module)

No code changes required for message receiving! The existing JMS listener works with Azure Service Bus:

```java
@Component
public class MessageListener {
    
    @JmsListener(destination = "${queue.name}")
    public void receiveMessage(Message message) {
        // Process message
    }
}
```

---

## Azure Resource Setup

### Step 1: Create Service Bus Namespace

```bash
# Set variables
RESOURCE_GROUP="myapp-rg"
LOCATION="eastus"
NAMESPACE_NAME="mycompany-servicebus"

# Create resource group (if not exists)
az group create --name $RESOURCE_GROUP --location $LOCATION

# Create Service Bus namespace (Premium tier for managed identity)
az servicebus namespace create \
    --resource-group $RESOURCE_GROUP \
    --name $NAMESPACE_NAME \
    --location $LOCATION \
    --sku Premium
```

### Step 2: Create Queue

```bash
# Create queue
QUEUE_NAME="thumbnail-queue"

az servicebus queue create \
    --resource-group $RESOURCE_GROUP \
    --namespace-name $NAMESPACE_NAME \
    --name $QUEUE_NAME \
    --max-size 1024
```

### Step 3: Configure Managed Identity

#### Option A: System-Assigned Managed Identity

```bash
# Enable system-assigned identity on your App Service
az webapp identity assign \
    --resource-group $RESOURCE_GROUP \
    --name myapp-web

# Get the principal ID
PRINCIPAL_ID=$(az webapp identity show \
    --resource-group $RESOURCE_GROUP \
    --name myapp-web \
    --query principalId \
    --output tsv)

# Assign Service Bus Data Sender role
az role assignment create \
    --assignee $PRINCIPAL_ID \
    --role "Azure Service Bus Data Sender" \
    --scope "/subscriptions/<subscription-id>/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.ServiceBus/namespaces/$NAMESPACE_NAME"
```

#### Option B: User-Assigned Managed Identity

```bash
# Create user-assigned identity
IDENTITY_NAME="myapp-identity"

az identity create \
    --resource-group $RESOURCE_GROUP \
    --name $IDENTITY_NAME

# Get identity info
CLIENT_ID=$(az identity show \
    --resource-group $RESOURCE_GROUP \
    --name $IDENTITY_NAME \
    --query clientId \
    --output tsv)

PRINCIPAL_ID=$(az identity show \
    --resource-group $RESOURCE_GROUP \
    --name $IDENTITY_NAME \
    --query principalId \
    --output tsv)

# Assign identity to App Service
az webapp identity assign \
    --resource-group $RESOURCE_GROUP \
    --name myapp-web \
    --identities "/subscriptions/<subscription-id>/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.ManagedIdentity/userAssignedIdentities/$IDENTITY_NAME"

# Assign Service Bus Data Sender role
az role assignment create \
    --assignee $PRINCIPAL_ID \
    --role "Azure Service Bus Data Sender" \
    --scope "/subscriptions/<subscription-id>/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.ServiceBus/namespaces/$NAMESPACE_NAME"

# Set environment variable
az webapp config appsettings set \
    --resource-group $RESOURCE_GROUP \
    --name myapp-web \
    --settings AZURE_CLIENT_ID=$CLIENT_ID
```

### Step 4: Required RBAC Roles

| Application | Role | Purpose |
|-------------|------|---------|
| Web (Producer) | Azure Service Bus Data Sender | Send messages to queues |
| Worker (Consumer) | Azure Service Bus Data Receiver | Receive messages from queues |

---

## Testing

### Local Testing

For local development, you can use connection strings (not recommended for production):

```properties
# For local testing only (use managed identity in production)
spring.cloud.azure.servicebus.connection-string=${SERVICE_BUS_CONNECTION_STRING}
```

Get connection string:
```bash
az servicebus namespace authorization-rule keys list \
    --resource-group $RESOURCE_GROUP \
    --namespace-name $NAMESPACE_NAME \
    --name RootManageSharedAccessKey \
    --query primaryConnectionString \
    --output tsv
```

### Integration Testing

Run the application with the `azure` profile:

```bash
# Web application
cd web
mvn spring-boot:run -Dspring-boot.run.profiles=azure

# Worker application (in another terminal)
cd worker
mvn spring-boot:run -Dspring-boot.run.profiles=azure
```

### Verification Steps

1. **Upload an image** through the web interface
2. **Check Service Bus queue** for messages:
   ```bash
   az servicebus queue show \
       --resource-group $RESOURCE_GROUP \
       --namespace-name $NAMESPACE_NAME \
       --name $QUEUE_NAME \
       --query countDetails
   ```
3. **Verify worker processing** by checking logs
4. **Confirm thumbnail creation** in the storage

---

## Deployment

### Environment Variables

Set these environment variables in your deployment environment:

| Variable | Description | Required |
|----------|-------------|----------|
| `SERVICE_BUS_NAMESPACE` | Service Bus namespace name | Yes |
| `AZURE_CLIENT_ID` | Client ID for user-assigned identity | Only for user-assigned MI |
| `SPRING_PROFILES_ACTIVE` | Should include `azure` | Yes |

### App Service Deployment

```bash
# Deploy web application
az webapp deployment source config-zip \
    --resource-group $RESOURCE_GROUP \
    --name myapp-web \
    --src web/target/web-0.0.1-SNAPSHOT.jar

# Configure environment
az webapp config appsettings set \
    --resource-group $RESOURCE_GROUP \
    --name myapp-web \
    --settings \
        SERVICE_BUS_NAMESPACE=$NAMESPACE_NAME \
        SPRING_PROFILES_ACTIVE=azure

# Deploy worker application
az webapp deployment source config-zip \
    --resource-group $RESOURCE_GROUP \
    --name myapp-worker \
    --src worker/target/worker-0.0.1-SNAPSHOT.jar

# Configure environment
az webapp config appsettings set \
    --resource-group $RESOURCE_GROUP \
    --name myapp-worker \
    --settings \
        SERVICE_BUS_NAMESPACE=$NAMESPACE_NAME \
        SPRING_PROFILES_ACTIVE=azure
```

---

## Troubleshooting

### Common Issues

#### 1. Authentication Failures

**Error**: `MsalServiceException: Managed Identity authentication is not available`

**Solutions**:
- Verify managed identity is enabled on the App Service
- Check RBAC role assignments
- Ensure `spring.cloud.azure.credential.managed-identity-enabled=true`

#### 2. Connection Issues

**Error**: `ServiceBusException: Failed to connect to Service Bus`

**Solutions**:
- Verify Service Bus namespace name is correct
- Check network connectivity (firewall rules, NSG)
- Ensure Service Bus namespace is in the same region

#### 3. Message Not Received

**Symptoms**: Messages sent but not received by worker

**Solutions**:
- Check queue name matches in both web and worker
- Verify worker is running with `azure` profile
- Check Service Bus queue metrics for message count
- Review worker application logs for errors

#### 4. Profile Not Active

**Error**: `No qualifying bean of type 'ServiceBusConfig'`

**Solutions**:
- Ensure `-Dspring.profiles.active=azure` is set
- Check that `@Profile("azure")` annotation is present on config classes
- Verify application-azure.properties exists and is in classpath

### Monitoring

#### Azure Portal
- Navigate to Service Bus namespace
- View queue metrics (message count, throughput)
- Check connection attempts and errors

#### Application Logs
- Enable DEBUG logging for Azure SDK:
  ```properties
  logging.level.com.azure=DEBUG
  logging.level.org.springframework.jms=DEBUG
  ```

#### Health Check
Add Service Bus health check:
```java
@Component
public class ServiceBusHealthIndicator implements HealthIndicator {
    
    @Autowired
    private ConnectionFactory connectionFactory;
    
    @Override
    public Health health() {
        try {
            connectionFactory.createConnection().close();
            return Health.up().build();
        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
    }
}
```

---

## Best Practices

### Security
1. **Always use managed identity** in production (no connection strings)
2. **Use Premium tier** for production workloads (Standard tier doesn't support managed identity)
3. **Limit RBAC roles** to minimum required (Data Sender/Receiver, not full access)
4. **Rotate credentials** if using connection strings for local development

### Performance
1. **Reuse JMS connections** (handled by Spring)
2. **Use message batching** for high throughput scenarios
3. **Configure appropriate prefetch** settings for consumers
4. **Monitor queue length** and add more workers if needed

### Reliability
1. **Configure dead-letter queues** for message failure handling
2. **Set appropriate message TTL** (time-to-live)
3. **Enable duplicate detection** if needed
4. **Monitor Service Bus metrics** for proactive issue detection

### Cost Optimization
1. **Right-size namespace tier** based on workload
2. **Use Basic/Standard tier** for dev/test (Premium for production with MI)
3. **Monitor message throughput** to optimize pricing tier
4. **Clean up old messages** to manage storage costs

---

## Additional Resources

- [Azure Service Bus Documentation](https://docs.microsoft.com/azure/service-bus-messaging/)
- [Spring Cloud Azure Service Bus](https://docs.microsoft.com/azure/developer/java/spring-framework/configure-spring-boot-starter-java-app-with-azure-service-bus)
- [Managed Identity Overview](https://docs.microsoft.com/azure/active-directory/managed-identities-azure-resources/)
- [Azure Service Bus Samples](https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/servicebus/azure-messaging-servicebus)

---

## Support

For issues or questions:
1. Check the [Troubleshooting](#troubleshooting) section
2. Review Azure Service Bus documentation
3. Check Application Insights for detailed error traces
4. Contact your Azure support team

---

**Migration Completed**: 2026-02-06  
**Version**: 1.0
