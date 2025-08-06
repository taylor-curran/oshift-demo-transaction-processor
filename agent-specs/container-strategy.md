# Container Strategy for Transaction Processor Service

## Overview

This document outlines the containerization strategy for the `transaction-processor` Spring Boot application, designed to support high-throughput banking transaction processing in a containerized environment.

## Base Image Selection

### Chosen Base Image: `eclipse-temurin:17-jdk-alpine`

**Rationale:**
- **Eclipse Temurin**: Industry-standard OpenJDK distribution with enterprise support and security updates
- **Java 17**: Matches the application's Java version requirement from pom.xml
- **Alpine Linux**: Minimal attack surface with smaller image size (~200MB vs ~400MB for Ubuntu-based images)
- **JDK vs JRE**: Using JDK for debugging capabilities and potential runtime compilation optimizations in high-throughput scenarios

**Alternative Considerations:**
- `openjdk:17-jre-alpine`: Smaller but lacks debugging tools needed for production troubleshooting
- `amazoncorretto:17`: AWS-optimized but adds vendor lock-in
- `eclipse-temurin:17-jre-alpine`: Considered but JDK provides better operational flexibility

## JVM Optimization Strategy

### Memory Configuration
```
-Xmx1536m
```
- **Heap Size**: 1536MB maximum heap, leaving ~512MB for non-heap memory in 2GB container
- **Rationale**: Matches Cloud Foundry configuration while accounting for container overhead

### Garbage Collection Optimization
```
-XX:+UseG1GC -XX:MaxGCPauseMillis=200
```
- **G1 Garbage Collector**: Optimized for low-latency, high-throughput applications
- **Pause Time Target**: 200ms maximum pause time to maintain transaction processing SLAs
- **Benefits**: Better suited for large heap sizes and concurrent processing than default collectors

## Security Configuration

### Non-Root User Implementation
- **User**: `appuser` (UID 1001) in `appgroup` (GID 1001)
- **Rationale**: Follows security best practices and banking compliance requirements
- **File Permissions**: Application files owned by non-root user to prevent privilege escalation

### Container Security Considerations
- Minimal base image reduces attack surface
- No unnecessary packages or tools installed
- Application runs with minimal required privileges

## Health Check Strategy

### Health Check Configuration
```dockerfile
HEALTHCHECK --interval=30s --timeout=15s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
```

**Parameters:**
- **Interval**: 30s between health checks (balanced frequency)
- **Timeout**: 15s timeout matching Cloud Foundry configuration
- **Start Period**: 60s grace period for application startup
- **Retries**: 3 failed attempts before marking unhealthy

**Endpoint**: `/actuator/health` - Spring Boot Actuator endpoint providing comprehensive health status

## High-Throughput Processing Support

### Thread Pool Configuration
The container supports high-throughput processing through environment variables:
- `PROCESSOR_THREAD_POOL_SIZE=50`: Concurrent processing threads
- `BATCH_SIZE=1000`: Transactions per batch
- `PROCESSING_TIMEOUT_MS=5000`: Maximum processing time per batch

### Resource Allocation
- **CPU**: No limits set to allow burst processing during peak loads
- **Memory**: 1536MB heap + ~512MB non-heap = ~2GB total (matches Cloud Foundry allocation)
- **Network**: Port 8080 exposed for HTTP traffic

## Environment Variable Strategy

### Configuration Management
The container expects the following environment variables for production deployment:

**Spring Configuration:**
- `SPRING_PROFILES_ACTIVE=production,high-throughput`

**Processing Configuration:**
- `PROCESSOR_THREAD_POOL_SIZE=50`
- `BATCH_SIZE=1000`
- `PROCESSING_TIMEOUT_MS=5000`

**Kafka Configuration:**
- `KAFKA_BOOTSTRAP_SERVERS`
- `KAFKA_CONSUMER_GROUP=transaction-processors`
- `KAFKA_AUTO_OFFSET_RESET=earliest`
- `KAFKA_MAX_POLL_RECORDS=500`

**Database Configuration:**
- `DB_SHARD_COUNT=4`
- `DB_WRITE_TIMEOUT=3000`
- `DB_READ_TIMEOUT=1000`

**Circuit Breaker Configuration:**
- `HYSTRIX_ENABLED=true`
- `CIRCUIT_BREAKER_THRESHOLD=20`

### ConfigMap/Secret Strategy
- **ConfigMaps**: Non-sensitive configuration (thread pools, timeouts, feature flags)
- **Secrets**: Database credentials, Kafka authentication, external service keys
- **Environment-specific**: Different values per environment (dev/staging/prod)

## Container Logging Strategy

### Logging Configuration
- **Standard Output**: All application logs written to stdout/stderr for container orchestration
- **Log Format**: JSON structured logging for enterprise log aggregation
- **Log Levels**: Configurable via `LOGGING_LEVEL_ROOT` environment variable

### Enterprise Integration
- **Log Aggregation**: Compatible with ELK stack, Splunk, or OpenShift logging
- **Audit Trails**: Transaction processing logs for compliance requirements
- **Metrics Integration**: Application metrics exposed via `/actuator/metrics` endpoint

## Deployment Considerations

### Container Orchestration
- **Kubernetes/OpenShift**: Designed for orchestrated deployment
- **Scaling**: Horizontal pod autoscaling based on CPU/memory metrics
- **Rolling Updates**: Zero-downtime deployments with health check integration

### Service Dependencies
The containerized application requires the following external services:
- Kafka cluster for message processing
- PostgreSQL database shards (4 instances)
- Redis cluster for caching
- Audit service for compliance logging
- Metrics collector for operational monitoring

### Network Configuration
- **Port**: 8080 (HTTP)
- **Health Check**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Service Mesh**: Compatible with Istio/OpenShift Service Mesh

## Migration from Cloud Foundry

### Key Differences
- **Service Bindings**: Replace with Kubernetes Services and ConfigMaps/Secrets
- **Health Checks**: Container health checks replace CF health check configuration
- **Scaling**: Kubernetes HPA replaces CF instance scaling
- **Logging**: Container stdout/stderr replaces CF log aggregation

### Compatibility
- **Environment Variables**: All CF environment variables supported
- **Health Endpoints**: Same endpoints and timeouts maintained
- **Resource Allocation**: Memory and processing configuration preserved
