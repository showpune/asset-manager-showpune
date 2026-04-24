## Supported Task Patterns

The following are the task patterns supported by the modernize CLI. These patterns are used to identify the modernization tasks that need to be performed based on the user's input.

The patterns are categorized into two groups, and they should be treated differently if picked:

* Patterns with skill definitions: These patterns have pre-defined skills that can be used to execute the tasks. If a task matches one of these patterns, the corresponding skill should be used in the task plan.
* Patterns without skill definitions: These patterns do not have pre-defined skills. If a task matches one of these patterns, the execution prompt should be used to guide the AI in performing the required tasks.
   **IMPORTANT**: The pattern name or execution prompt should NEVER be used as the skill name in the generated plan and tasks.json. They are meant to guide the task generation, not to be directly used as skills.


### Task Patterns with Skill Definitions
These patterns have pre-defined skills to assist in their execution. When they are selected in a modernization plan, the corresponding skills should be used.
Each of the item is written in the following format: `- **skill-name**: skill-description`.

- **migration-AWS-secrets-manager-to-azure-key-vault**: Migrate from AWS Secrets Manager to Azure Key Vault to securely manage and access sensitive information in Azure.
- **migration-activemq-servicebus**: Migrate from ActiveMQ Artemis to Azure Service Bus for messaging.
- **migration-amqp-rabbitmq-servicebus**: Migrate from RabbitMQ with AMQP to Azure Service Bus for messaging.
- **migration-ant-project-to-maven-project**: Migrate current project from Ant project to Maven project
- **migration-apache-commons-jcs-to-azure-cache-for-redis**: Migrate from Apache Commons JCS in-process cache to Azure Cache for Redis.
- **migration-apache-ignite-to-azure-cache-redis**: Migrate from Apache Ignite distributed cache to Azure Cache for Redis.
- **migration-certificate-management-to-azure-key-vault**: Migrate from a local KeyStore to Azure Key Vault for secure storage and access to certificates and keys.
- **migration-containerization-copilot-agent**: The app does not have a Dockerfile and/or is not container-ready. Use Agent Mode with Copilot to create and execute a containerization plan.
- **migration-deprecated-api-upgrade**: Upgrade deprecated APIs to their recommended alternatives for improved security, performance, and compatibility.
- **migration-dynacache-to-azure-cache-for-redis**: Migrate from DynaCache to Azure Cache for Redis.
- **migration-eclipse-project-to-maven-project**: Migrate current project from eclipse project to maven project
- **migration-ehcache-to-azure-cache-for-redis**: Migrate from Ehcache in-process cache to Azure Cache for Redis.
- **migration-embedded-cache-to-azure-cache-for-redis**: Migrate from embedded cache to Azure Cache for Redis for a scalable, managed solution that easily integrates with other Azure services.
- **migration-hazelcast-to-azure-cache-for-redis**: Migrate from Hazelcast distributed cache to Azure Cache for Redis.
- **migration-infinispan-to-azure-cache-redis**: Migrate from Infinispan distributed cache to Azure Cache for Redis.
- **migration-jakarta-ee-upgrade**: Upgrade to the latest stable version of Jakarta EE for improved security, performance, and compatibility.
- **migration-java-ee-amqp-rabbitmq-servicebus**: Migrate from RabbitMQ with AMQP to Azure Service Bus for messaging in Java EE/Jakarta EE applications.
- **migration-java-version-upgrade**: Upgrade to the latest stable version of Java for improved security, performance, and compatibility.
- **migration-javax.email-send-to-azure-communication-service-email**: Migrate from Javax Email to Azure Communication Service for sending emails.
- **migration-jax-rpc-to-jax-ws**: Migrate from JAX-RPC to JAX-WS for web services. JAX-RPC is deprecated and JAX-WS is the recommended alternative.
- **migration-jboss-cache-to-azure-cache-for-redis**: Migrate from JBoss Cache to Azure Cache for Redis.
- **migration-jcache-to-azure-cache-for-redis**: Migrate from JCache (JSR-107) to Azure Cache for Redis for a scalable, managed solution that easily integrates with other Azure services.
- **migration-kafka-to-eventhubs**: Migrate from Kafka to Azure Event Hubs for Apache Kafka with managed identity for secure, credential-free authentication.
- **migration-local-files-to-mounted-azure-storage**: Migrate from local file system to Azure Storage Account File Share mounts for scalable and secure file storage.
- **migration-local-redis-to-azure-redis-cache**: Migrate from a local Redis instance to Azure Cache for Redis.
- **migration-local-session-to-azure-redis-cache**: Migrate from local session storage to Azure Cache for Redis for better scalability and distributed session management.
- **migration-log-to-console**: Migrate from file-based logging to console logging to support cloud-native apps and integration with Azure Monitor.
- **migration-memcached-to-azure-cache-for-redis**: Migrate from Memcached distributed cache to Azure Cache for Redis.
- **migration-mi-azuresql-azure-sdk-21v-china**: Migrate from SQL Database to Azure SQL Database with Azure SDK and managed identity in Mooncake for secure, credential-free authentication.
- **migration-mi-azuresql-azure-sdk-public-cloud**: Migrate from SQL Database to Azure SQL Database with Azure SDK and managed identity for secure, credential-free authentication.
- **migration-mi-cassandra-azure-sdk-public-cloud**: Migrate from Cassandra to Azure Cosmos DB for Cassandra for a fully managed, scalable database with Cassandra API support.
- **migration-mi-mariadb-azure-sdk-public-cloud**: Migrate from MariaDB to Azure Database for MariaDB with managed identity for secure, credential-free authentication.
- **migration-mi-mongodb-azure-sdk-public-cloud**: Migrate from MongoDB to Azure Cosmos DB for MongoDB with managed identity for a fully managed, scalable database service with MongoDB API support.
- **migration-mi-mysql-azure-sdk-21v-china**: Migrate from MySQL to Azure Database for MySQL with Azure SDK and managed identity in the Mooncake cloud for secure, credential-free authentication.
- **migration-mi-mysql-azure-sdk-public-cloud**: Migrate from MySQL to Azure Database for MySQL with Azure SDK and managed identity for secure, credential-free authentication.
- **migration-mi-postgresql-azure-sdk-21v-china**: Migrate from PostgreSQL to Azure Database for PostgreSQL with Azure SDK and managed identity in the Mooncake cloud for secure, credential-free authentication.
- **migration-mi-postgresql-azure-sdk-public-cloud**: Migrate from PostgreSQL to Azure Database for PostgreSQL with Azure SDK and managed identity for secure, credential-free authentication.
- **migration-on-premises-user-authentication-to-microsoft-entra-id**: Migrate the user authentication to Microsoft Entra ID authentication
- **migration-oracle-coherence-to-azure-cache-for-redis**: Migrate from Oracle Coherence distributed cache to Azure Cache for Redis.
- **migration-oracle-to-postgresql**: Migrate from Oracle DB to PostgreSQL
- **migration-oscache-to-azure-cache-for-redis**: Migrate from OSCache in-process cache to Azure Cache for Redis.
- **migration-plaintext-credential-to-azure-keyvault**: Migrate from plaintext credentials in the code to Azure Key Vault for storage and access to sensitive information.
- **migration-s3-to-azure-blob-storage**: Migrate from AWS S3 to Azure Blob Storage for scalable and secure object storage in Azure.
- **migration-shift-one-to-azure-cache-for-redis**: Migrate from ShiftOne in-process cache to Azure Cache for Redis.
- **migration-spring-boot-upgrade**: Upgrade to the latest stable version of Spring Boot for improved security, performance, and compatibility.
- **migration-spring-cache-backends-to-spring-cache-with-azure-cache-for-redis**: Migrate from Spring Cache to Azure Cache for Redis for a scalable, managed solution that easily integrates with other Azure services.
- **migration-spring-framework-upgrade**: Upgrade to the latest stable version of Spring Framework for improved security, performance, and compatibility.
- **migration-spring-jms-rabbitmq-servicebus**: Migrate from RabbitMQ with JMS to Azure Service Bus for a managed messaging service with JMS API support.
- **migration-sqs-to-servicebus**: Migrate from AWS Simple Queue Service to Azure Service Bus for a managed messaging service with advanced features.
- **migration-swarm-cache-to-azure-cache-for-redis**: Migrate from Swarm Cache to Azure Cache for Redis.

### Task Patterns without Skill Definitions
These patterns DO NOT have pre-defined skills. The `Pattern` defines the modernization scenario, NOT A SKILL. They are in the format of `- **Pattern**: Execution Prompt`.

A pattern should be selected if it matches one of the customer's requirements, and there is no skills supporting this requirement. The prompt should be added to the task plan to guide the AI in performing the required tasks.

**IMPORTANT**: NEVER write the pattern or execution-prompt as skill name in the generated plan.

- **Amazon Kinesis to Azure Event Hubs**: Migrate this application from using Amazon Kinesis to Azure Event Hubs. Ensure all usages including both code and configuration are migrated and build passed.
- **Amazon SNS to Azure Service Bus**: Migrate this application from using Amazon SNS to Azure Service Bus. Ensure all usages including both code and configuration are migrated and build passed.
- **Apache Pulsar to Azure Event Hubs**: Migrate this application from using Apache Pulsar to Azure Event Hubs. Ensure all usages including both code and configuration are migrated and build passed.
- **AWS Lambda to Azure Functions**: Migrate this application from using AWS Lambda to Azure Functions. Ensure all usages including both code and configuration are migrated and build passed.
- **Firebird to Azure PostgreSQL**: Migrate this application from using Firebird to Azure PostgreSQL. Ensure all usages including both code and configuration are migrated and build passed.
- **Google Cloud Bigtable to Azure Cosmos DB**: Migrate this application from using Google Cloud Bigtable to Azure Cosmos DB. Ensure all usages including both code and configuration are migrated and build passed.
- **Google Cloud Functions to Azure Functions**: Migrate this application from using Google Cloud Functions to Azure Functions. Ensure all usages including both code and configuration are migrated and build passed.
- **Google Cloud Pub/Sub to Azure Service Bus**: Migrate this application from using Google Cloud Pub/Sub to Azure Service Bus. Ensure all usages including both code and configuration are migrated and build passed.
- **Google Cloud Spanner to Azure PostgreSQL**: Migrate this application from using Google Cloud Spanner to Azure PostgreSQL. Ensure all usages including both code and configuration are migrated and build passed.
- **Google Cloud Storage to Azure Blob Storage**: Migrate this application from using Google Cloud Storage to Azure Blob Storage. Ensure all usages including both code and configuration are migrated and build passed.
- **Google Firestore to Azure Cosmos DB**: Migrate this application from using Google Firestore to Azure Cosmos DB. Ensure all usages including both code and configuration are migrated and build passed.
- **IBM DB2 to Azure PostgreSQL**: Migrate this application from using IBM DB2 to Azure PostgreSQL. Ensure all usages including both code and configuration are migrated and build passed.
- **IBM MQ JMS to Azure Service Bus**: Migrate this application from using IBM MQ JMS to Azure Service Bus. Ensure all usages including both code and configuration are migrated and build passed.
- **Quartz Scheduler to Azure Functions**: Migrate this application from using Quartz Scheduler to Azure Functions. Ensure all usages including both code and configuration are migrated and build passed.
- **Solace PubSub+ to Azure Service Bus**: Migrate this application from using Solace PubSub+ to Azure Service Bus. Ensure all usages including both code and configuration are migrated and build passed.
- **Spring Batch to Azure Durable Functions**: Migrate this application from using Spring Batch to Azure Durable Functions. Ensure all usages including both code and configuration are migrated and build passed.
- **Spring Cloud Config to Azure App Configuration**: Migrate this application from using Spring Cloud Config to Azure App Configuration. Ensure all usages including both code and configuration are migrated and build passed.
- **SQLite to Azure PostgreSQL**: Migrate this application from using SQLite to Azure PostgreSQL. Ensure all usages including both code and configuration are migrated and build passed.
- **Sybase ASE to Azure PostgreSQL**: Migrate this application from using Sybase ASE to Azure PostgreSQL. Ensure all usages including both code and configuration are migrated and build passed.
- **TIBCO EMS JMS to Azure Service Bus**: Migrate this application from using TIBCO EMS JMS to Azure Service Bus. Ensure all usages including both code and configuration are migrated and build passed.
