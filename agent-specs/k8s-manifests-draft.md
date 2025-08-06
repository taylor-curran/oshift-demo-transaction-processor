# Kubernetes Manifests for Transaction Processor

**Ticket:** OSM-29 - Create Kubernetes Manifests for Transaction Processor  
**Application:** transaction-processor  
**Date:** August 6, 2025  
**Author:** Devin AI  

## Overview

This document describes the Kubernetes manifests created for the transaction-processor application as part of the migration from Cloud Foundry to Kubernetes. The manifests follow established organizational patterns and security standards while maintaining the high-throughput processing capabilities required for banking transaction processing.

## Manifest Files

### Core Application Manifests

#### 1. Deployment (`k8s/deployment.yaml`)

The main deployment manifest configures the transaction-processor application with:

- **Replicas:** 5 instances matching the Cloud Foundry configuration
- **Resource Limits:** 
  - CPU: 500m request, 2000m limit (optimized for high-throughput processing)
  - Memory: 1.2Gi request, 2Gi limit (matching CF 2GB allocation)
- **Security Context:** Non-root user (UID 1001), read-only filesystem, dropped capabilities
- **Health Probes:**
  - Liveness: `/actuator/health/liveness` (30s initial delay, 15s timeout)
  - Readiness: `/actuator/health/readiness` (10s initial delay, 15s timeout)
- **Environment Variables:** All CF environment variables preserved
- **Service Integration:** References to all ConfigMaps and Secrets via `envFrom`

Key features:
- Uses `registry.bank.internal/transaction-processor:2.0.1` image (no `:latest` tag)
- Prometheus metrics scraping enabled on port 8080
- Temporary storage volumes for application logs and temp files

#### 2. Service (`k8s/service.yaml`)

Internal ClusterIP service for pod communication:
- **Type:** ClusterIP (internal only)
- **Port:** 8080 (HTTP)
- **Selector:** Targets pods with `app.kubernetes.io/name: transaction-processor`

#### 3. HPA (`k8s/hpa.yaml`)

Horizontal Pod Autoscaler for dynamic scaling:
- **Min Replicas:** 5 (baseline capacity)
- **Max Replicas:** 15 (peak load capacity)
- **Metrics:** 
  - CPU utilization: 70% threshold
  - Memory utilization: 80% threshold
- **Scaling Behavior:** 
  - Scale up: 50% increase every 60s
  - Scale down: 25% decrease every 60s with 5-minute stabilization

#### 4. Ingress (`k8s/ingress.yaml`)

Internal routing configuration:
- **Host:** `transaction-processor.internal.banking.com`
- **TLS:** Enabled with certificate from `transaction-processor-tls` secret
- **Path:** Root path (`/`) with Prefix matching
- **Annotations:** SSL redirect enabled, nginx ingress controller

### Configuration Resources

#### Database Sharding Configuration

Four separate ConfigMap/Secret pairs for PostgreSQL database shards:

**ConfigMaps** (`k8s/samples/configmaps/pe-eng-transaction-processor-db-shard-[1-4]-config.yaml`):
- Database connection parameters (host, port, database name)
- Connection pooling settings (max connections: 25)
- Timeout configurations (write: 3000ms, read: 1000ms)
- SSL requirements and application name

**Secrets** (`k8s/samples/secrets/pe-eng-transaction-processor-db-shard-[1-4]-secret.yaml`):
- Database credentials (username, password)
- Base64 encoded placeholder values for security

#### Kafka Configuration

**ConfigMap** (`k8s/samples/configmaps/pe-eng-transaction-processor-kafka-config.yaml`):
- Broker endpoints and consumer group settings
- Performance tuning (max poll records: 500, batch size: 1000)
- Reliability settings (acks: all, retries: 3)

**Secret** (`k8s/samples/secrets/pe-eng-transaction-processor-kafka-secret.yaml`):
- Authentication credentials and SSL certificates

#### Redis Cluster Configuration

**ConfigMap** (`k8s/samples/configmaps/pe-eng-transaction-processor-redis-cluster-config.yaml`):
- Cluster node endpoints and connection settings
- Performance parameters (max connections: 50, timeout: 5000ms)

**Secret** (`k8s/samples/secrets/pe-eng-transaction-processor-redis-cluster-secret.yaml`):
- Authentication password and tokens

#### External Services Configuration

**Audit Service:**
- ConfigMap: Service endpoint, timeout, and retry configuration
- Secret: API key and client secret for authentication

**Metrics Collector:**
- ConfigMap: Collection endpoint, intervals, and batch sizes
- Secret: API key and authentication tokens

## Design Decisions

### Naming Convention
All resources follow the pattern: `pe-eng-transaction-processor-<service>-<type>`
- **Team:** `pe-eng` (Platform Engineering)
- **Application:** `transaction-processor`
- **Environment:** `dev` (for sample configurations)

### Security Implementation
- **Non-root execution:** All containers run as UID 1001
- **Read-only filesystem:** Prevents runtime file modifications
- **Capability dropping:** All Linux capabilities removed
- **Seccomp profile:** RuntimeDefault for syscall filtering
- **Secret separation:** Credentials isolated from configuration

### Resource Strategy
- **CPU allocation:** 500m request ensures guaranteed resources, 2000m limit allows burst processing
- **Memory allocation:** 1.2Gi request with 2Gi limit matches CF configuration
- **Scaling strategy:** 5-15 replica range supports baseline to peak load scenarios

### Health Check Strategy
- **Separate endpoints:** Liveness and readiness probes use different endpoints
- **Appropriate timeouts:** 15s timeout matches CF configuration
- **Startup grace period:** 30s liveness delay allows for application initialization

## Environment Variable Mapping

The following Cloud Foundry environment variables are preserved in the Kubernetes deployment:

### Application Configuration
- `SPRING_PROFILES_ACTIVE`: production,high-throughput
- `JBP_CONFIG_OPEN_JDK_JRE`: Java 17 runtime configuration
- `JVM_OPTS`: G1GC with 1536m heap and 200ms pause target

### High-Throughput Processing
- `PROCESSOR_THREAD_POOL_SIZE`: 50 concurrent threads
- `BATCH_SIZE`: 1000 transactions per batch
- `PROCESSING_TIMEOUT_MS`: 5000ms maximum processing time

### Database Sharding
- `DB_SHARD_COUNT`: 4 PostgreSQL shards
- `DB_WRITE_TIMEOUT`: 3000ms write timeout
- `DB_READ_TIMEOUT`: 1000ms read timeout

### Circuit Breaker
- `HYSTRIX_ENABLED`: true
- `CIRCUIT_BREAKER_THRESHOLD`: 20% error threshold

## Compliance and Standards

### Kubernetes Standards Compliance
- **Image Provenance:** Uses internal registry with pinned tags
- **Resource Limits:** CPU and memory requests/limits enforced
- **Security Context:** Non-root, read-only filesystem, capability dropping
- **Naming and Labels:** Consistent labeling with mandatory fields

### Banking Compliance
- **Audit Integration:** Comprehensive audit service configuration
- **Metrics Collection:** Operational monitoring and compliance reporting
- **Security Controls:** Multi-layer security with secrets management

## Testing and Validation

### Validation Commands
```bash
# Syntax validation
kubectl apply --dry-run=client -f k8s/

# Resource validation
kubectl apply --dry-run=server -f k8s/

# Application compilation
mvn compile

# Unit tests
mvn test
```

### KIND Cluster Testing
```bash
# Create local cluster
kind create cluster --name transaction-processor-test

# Apply manifests
kubectl apply -f k8s/

# Verify deployment
kubectl get pods,services,hpa,ingress
kubectl describe deployment pe-eng-transaction-processor-dev
```

## Migration Considerations

### From Cloud Foundry
- **Service Bindings:** Replaced with Kubernetes Services and ConfigMaps/Secrets
- **Health Checks:** Container health checks replace CF health check configuration
- **Scaling:** HPA replaces CF instance scaling
- **Routing:** Ingress replaces CF routes

### Operational Changes
- **Log Aggregation:** Container stdout/stderr replaces CF log streams
- **Metrics Collection:** Prometheus scraping replaces CF metrics
- **Service Discovery:** Kubernetes DNS replaces CF service discovery

## Next Steps

1. **Environment-Specific Configuration:** Create prod/staging variants with appropriate values
2. **Secret Management:** Integrate with external secret management systems
3. **Monitoring Setup:** Configure Prometheus and Grafana dashboards
4. **CI/CD Integration:** Automate deployment pipeline with GitOps
5. **Performance Testing:** Validate high-throughput processing under load
6. **Security Scanning:** Container and manifest security validation
7. **Disaster Recovery:** Backup and recovery procedures for Kubernetes deployment

## References

- [Cloud Foundry Manifest](../manifest.yml) - Original CF configuration
- [Binding Mapping Documentation](binding-mapping.md) - Service binding translation
- [Container Strategy](container-strategy.md) - Containerization approach
- [Credit Scoring Engine K8s Manifests](../../oshift-demo-credit-scoring-engine/k8s/) - Reference implementation
