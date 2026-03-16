// Main Bicep template – Asset Manager Kit Azure Migration
// Orchestrates all resource modules: identity, storage, service bus, and postgresql.

targetScope = 'resourceGroup'

// ─── Parameters ──────────────────────────────────────────────────────────────

@description('Azure region where all resources will be deployed')
param location string = resourceGroup().location

@description('Short environment name used as a suffix for all resource names (e.g. dev, staging, prod)')
@allowed(['dev', 'staging', 'prod'])
param environment string = 'dev'

@description('Unique suffix appended to globally-scoped resource names to avoid conflicts')
param resourceSuffix string = uniqueString(resourceGroup().id)

@description('Name of the blob container for uploaded assets')
param assetsContainerName string = 'assets'

@description('Name of the Service Bus queue for thumbnail generation requests')
param thumbnailQueueName string = 'thumbnail-requests'

@description('Name of the application database inside the PostgreSQL server')
param databaseName string = 'assetsdb'

@description('PostgreSQL administrator login name')
param postgresAdminLogin string = 'pgadmin'

@description('PostgreSQL administrator password')
@secure()
param postgresAdminPassword string

// ─── Variables ───────────────────────────────────────────────────────────────

var appName = 'assetmgr'
var prefix = '${appName}-${environment}'

var identityName        = '${prefix}-identity'
var storageAccountName  = '${appName}${environment}${take(resourceSuffix, 8)}'   // max 24 chars, lowercase
var serviceBusName      = '${prefix}-sb-${take(resourceSuffix, 8)}'
var postgresServerName  = '${prefix}-pg-${take(resourceSuffix, 8)}'

var commonTags = {
  application: 'Asset Manager Kit'
  environment: environment
  managedBy: 'bicep'
}

// ─── Modules ─────────────────────────────────────────────────────────────────

module identity 'modules/identity.bicep' = {
  name: 'deploy-identity'
  params: {
    location: location
    identityName: identityName
    tags: commonTags
  }
}

module storage 'modules/storage.bicep' = {
  name: 'deploy-storage'
  params: {
    location: location
    storageAccountName: storageAccountName
    containerName: assetsContainerName
    managedIdentityPrincipalId: identity.outputs.principalId
    tags: commonTags
  }
}

module servicebus 'modules/servicebus.bicep' = {
  name: 'deploy-servicebus'
  params: {
    location: location
    namespaceName: serviceBusName
    thumbnailQueueName: thumbnailQueueName
    managedIdentityPrincipalId: identity.outputs.principalId
    tags: commonTags
  }
}

module postgresql 'modules/postgresql.bicep' = {
  name: 'deploy-postgresql'
  params: {
    location: location
    serverName: postgresServerName
    databaseName: databaseName
    adminLogin: postgresAdminLogin
    adminPassword: postgresAdminPassword
    managedIdentityObjectId: identity.outputs.principalId
    managedIdentityName: identityName
    managedIdentityClientId: identity.outputs.clientId
    tags: commonTags
  }
}

// ─── Outputs ─────────────────────────────────────────────────────────────────

@description('Resource ID of the user-assigned managed identity')
output managedIdentityId string = identity.outputs.identityId

@description('Client ID of the managed identity – use this value in application configuration')
output managedIdentityClientId string = identity.outputs.clientId

@description('Primary blob service endpoint for the storage account')
output blobEndpoint string = storage.outputs.blobEndpoint

@description('Name of the blob container that stores uploaded assets')
output assetsContainerName string = storage.outputs.containerName

@description('Service Bus namespace hostname – use as the connection endpoint')
output serviceBusHostname string = servicebus.outputs.namespaceHostname

@description('Name of the thumbnail generation queue')
output thumbnailQueueName string = servicebus.outputs.thumbnailQueueName

@description('PostgreSQL server fully-qualified domain name')
output postgresServerFqdn string = postgresql.outputs.serverFqdn

@description('Name of the application database')
output postgresDatabaseName string = postgresql.outputs.databaseName

@description('JDBC connection string with Azure AD passwordless authentication plugin pre-configured')
output postgresJdbcConnectionString string = postgresql.outputs.jdbcConnectionString
