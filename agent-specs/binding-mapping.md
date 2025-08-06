# Service Binding Mapping for Transaction Processor

**Ticket:** OSM-28 - Map Service Bindings to K8s Config for App transaction-processor  
**Application:** transaction-processor  
**Date:** August 6, 2025  
**Analyst:** Devin AI  

## Overview

This document maps the 8 Cloud Foundry service bindings defined in `manifest.yml` to their corresponding Kubernetes ConfigMaps and Secrets. Each service binding is translated into appropriate K8s resources following the naming conventions and security best practices for the high-throughput transaction processing application.

The transaction-processor is a performance-critical application running 5 instances with 2GB memory each, requiring optimized configurations for database sharding, Kafka messaging, Redis caching, and comprehensive audit/metrics collection.

## Service Binding Mapping Table

| CF Service | K8s Resource Type | Secret/ConfigMap Name | Contents | Notes |
|------------|-------------------|----------------------|----------|-------|
| transaction-kafka | Secret + ConfigMap | pe-eng-transaction-processor-kafka-secret<br/>pe-eng-transaction-processor-kafka-config | **Secret:** username, password, ssl-cert<br/>**ConfigMap:** brokers, consumer-group, topics, batch-size | Kafka cluster for transaction message processing |
| transaction-db-shard-1 | Secret + ConfigMap | pe-eng-transaction-processor-db-shard-1-secret<br/>pe-eng-transaction-processor-db-shard-1-config | **Secret:** username, password<br/>**ConfigMap:** host, port, database, connection-params | PostgreSQL shard 1 for transaction data partitioning |
| transaction-db-shard-2 | Secret + ConfigMap | pe-eng-transaction-processor-db-shard-2-secret<br/>pe-eng-transaction-processor-db-shard-2-config | **Secret:** username, password<br/>**ConfigMap:** host, port, database, connection-params | PostgreSQL shard 2 for transaction data partitioning |
| transaction-db-shard-3 | Secret + ConfigMap | pe-eng-transaction-processor-db-shard-3-secret<br/>pe-eng-transaction-processor-db-shard-3-config | **Secret:** username, password<br/>**ConfigMap:** host, port, database, connection-params | PostgreSQL shard 3 for transaction data partitioning |
| transaction-db-shard-4 | Secret + ConfigMap | pe-eng-transaction-processor-db-shard-4-secret<br/>pe-eng-transaction-processor-db-shard-4-config | **Secret:** username, password<br/>**ConfigMap:** host, port, database, connection-params | PostgreSQL shard 4 for transaction data partitioning |
| transaction-redis-cluster | Secret + ConfigMap | pe-eng-transaction-processor-redis-cluster-secret<br/>pe-eng-transaction-processor-redis-cluster-config | **Secret:** password, auth-token<br/>**ConfigMap:** cluster-nodes, port, timeout, max-connections | Redis cluster for caching and session management |
| audit-service | Secret + ConfigMap | pe-eng-transaction-processor-audit-service-secret<br/>pe-eng-transaction-processor-audit-service-config | **Secret:** api-key, client-secret<br/>**ConfigMap:** base-url, timeout, retry-config | Audit service for compliance and transaction logging |
| metrics-collector | Secret + ConfigMap | pe-eng-transaction-processor-metrics-collector-secret<br/>pe-eng-transaction-processor-metrics-collector-config | **Secret:** api-key, auth-token<br/>**ConfigMap:** endpoint, collection-interval, batch-size | Metrics collection service for performance monitoring |

## Design Decisions

### Naming Convention
- **Pattern:** `pe-eng-transaction-processor-<service>-<type>`
- **Team:** `pe-eng` (Platform Engineering)
- **App:** `transaction-processor`
- **Environment:** `dev` (for sample files)

### Resource Separation Strategy
- **Secrets:** Store sensitive data (credentials, keys, certificates)
- **ConfigMaps:** Store non-sensitive configuration (endpoints, timeouts, parameters)
- **Granularity:** One Secret and one ConfigMap per service for clear separation

### Security Considerations
- All credential values use `BASE64_ENCODED_*_PLACEHOLDER` format
- Secrets are separated from configuration to enable different access controls
- Ready for integration with external secret management systems

### Label Strategy
All resources include mandatory labels:
- `app.kubernetes.io/name: transaction-processor`
- `app.kubernetes.io/version: "2.0.1"`
- `app.kubernetes.io/part-of: banking-platform`
- `environment: dev`
- `managed-by: helm`

## Configuration Parameters by Service

### Kafka Cluster (transaction-kafka)
- **Connection:** broker list, consumer group configuration
- **Credentials:** username, password, SSL certificates
- **Parameters:** batch size (500), auto offset reset, max poll records
- **Performance:** Optimized for high-throughput with 50 thread pool size

### Database Shards (transaction-db-shard-1 through 4)
- **Connection:** host, port, database name per shard
- **Credentials:** username, password per shard
- **Parameters:** connection pooling, write/read timeouts (3000ms/1000ms)
- **Sharding:** 4-shard configuration for horizontal scaling

### Redis Cluster (transaction-redis-cluster)
- **Connection:** cluster nodes, port configuration
- **Credentials:** password, auth token
- **Parameters:** timeout settings, max connections
- **Performance:** Optimized for high-frequency caching operations

### Audit Service
- **Connection:** service endpoint, timeout settings
- **Credentials:** API key, client secret
- **Parameters:** retry configuration, batch processing
- **Compliance:** Supports regulatory audit requirements

### Metrics Collector
- **Connection:** metrics endpoint, collection intervals
- **Credentials:** API key, authentication token
- **Parameters:** batch size, retention policies
- **Monitoring:** Performance metrics for 5-instance deployment

## Environment Variable Mapping

The following environment variables from `manifest.yml` will be sourced from these K8s resources:

### Application Configuration
- `SPRING_PROFILES_ACTIVE` → transaction-processor-config (production,high-throughput)
- `JVM_OPTS` → transaction-processor-config (-Xmx1536m -XX:+UseG1GC)

### High-Throughput Processing
- `PROCESSOR_THREAD_POOL_SIZE` → transaction-processor-config (50)
- `BATCH_SIZE` → transaction-processor-config (1000)
- `PROCESSING_TIMEOUT_MS` → transaction-processor-config (5000)

### Kafka Configuration
- `KAFKA_BOOTSTRAP_SERVERS` → kafka-config
- `KAFKA_CONSUMER_GROUP` → kafka-config
- `KAFKA_AUTO_OFFSET_RESET` → kafka-config
- `KAFKA_MAX_POLL_RECORDS` → kafka-config

### Database Configuration
- `DB_SHARD_COUNT` → db-shard-*-config (4)
- `DB_WRITE_TIMEOUT` → db-shard-*-config (3000)
- `DB_READ_TIMEOUT` → db-shard-*-config (1000)
- Database credentials → db-shard-*-secret

### Circuit Breaker Configuration
- `HYSTRIX_ENABLED` → transaction-processor-config (true)
- `CIRCUIT_BREAKER_THRESHOLD` → transaction-processor-config (20)

## Implementation Notes

1. **Performance Optimization:** Configurations reflect high-throughput requirements with 5 instances and 2GB memory allocation
2. **Database Sharding:** 4-shard configuration enables horizontal scaling for transaction volume
3. **Fault Tolerance:** Circuit breaker patterns and retry configurations for resilience
4. **Compliance:** Audit service integration supports regulatory requirements
5. **Monitoring:** Comprehensive metrics collection for operational visibility

## Next Steps

1. Deploy sample configurations to development environment
2. Implement service integration code to consume these configurations
3. Validate connectivity and configuration injection
4. Performance test with high-throughput transaction loads
5. Extend to staging and production environments with environment-specific values
