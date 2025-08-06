# Kubernetes Manifests for Transaction Processor

This document describes the Kubernetes manifests created for migrating the transaction-processor application from Cloud Foundry to Kubernetes, following organizational standards and best practices.

## Overview

The transaction-processor is a high-throughput banking transaction processing engine that requires:
- 5 replicas for high availability and load distribution
- 2Gi memory allocation for processing large transaction batches
- Database sharding across 4 PostgreSQL instances
- Kafka integration for real-time transaction streaming
- Circuit breaker patterns for system resilience

## Manifest Files

### 1. Deployment (`k8s/deployment.yaml`)

The deployment manifest configures the core application runtime with enterprise-grade settings:

#### Key Configuration
- **Replicas:** 5 instances matching Cloud Foundry configuration
- **Image:** `registry.bank.internal/transaction-processor:2.0.1` following image provenance rules
- **Resources:** 
  - CPU: 500m requests, 2000m limits (appropriate for high-throughput processing)
  - Memory: 1.2Gi requests, 2Gi limits (60% request-to-limit ratio)

#### Security Context (Windsurf Compliance)
- `runAsNonRoot: true` - Prevents root execution
- `seccompProfile.type: RuntimeDefault` - Applies default seccomp profile
- `readOnlyRootFilesystem: true` - Immutable container filesystem
- `capabilities.drop: ["ALL"]` - Removes all Linux capabilities

#### Health Probes
- **Liveness Probe:** `/actuator/health` with 15s timeout (matching CF configuration)
- **Readiness Probe:** `/actuator/health` with 15s timeout
- **Initial Delays:** 60s liveness, 30s readiness for application startup

#### Environment Variables
Direct environment variables from Cloud Foundry manifest:
- `SPRING_PROFILES_ACTIVE: "production,high-throughput"`
- `PROCESSOR_THREAD_POOL_SIZE: "50"`
- `BATCH_SIZE: "1000"`
- `PROCESSING_TIMEOUT_MS: "5000"`
- Kafka configuration (bootstrap servers, consumer group, polling settings)
- Database sharding configuration (4 shards, timeouts)
- Circuit breaker settings (Hystrix enabled, 20% threshold)

#### External Configuration
The deployment references 14 ConfigMaps and Secrets via `envFrom`:
- **Kafka:** `pe-eng-transaction-processor-kafka-config/secret`
- **Database Shards:** 4 pairs for `db-shard-1` through `db-shard-4`
- **Redis Cluster:** `pe-eng-transaction-processor-redis-cluster-config/secret`
- **Audit Service:** `pe-eng-transaction-processor-audit-service-config/secret`
- **Metrics Collector:** `pe-eng-transaction-processor-metrics-collector-config/secret`

#### Volume Mounts
- `/tmp` - Temporary storage for application operations
- `/tmp/app-logs` - Application log directory

#### Monitoring Integration
- Prometheus scraping enabled with annotations:
  - `prometheus.io/scrape: "true"`
  - `prometheus.io/port: "8080"`
  - `prometheus.io/path: "/metrics"`

### 2. Service (`k8s/service.yaml`)

Internal ClusterIP service for application communication:

#### Configuration
- **Type:** ClusterIP (internal access only)
- **Port:** 8080 (matching Spring Boot default)
- **Target Port:** 8080
- **Protocol:** TCP
- **Selector:** Matches deployment labels

#### Naming Convention
- Service name: `pe-eng-transaction-processor-dev`
- Follows `<team>-<app>-<env>` pattern

### 3. Horizontal Pod Autoscaler (`k8s/hpa.yaml`)

Auto-scaling configuration for dynamic load handling:

#### Scaling Parameters
- **Min Replicas:** 2 (baseline capacity)
- **Max Replicas:** 10 (peak load capacity)
- **CPU Target:** 80% utilization
- **Memory Target:** 80% utilization

#### Scaling Behavior
- **Scale Up:** 100% increase every 15 seconds (rapid response to load spikes)
- **Scale Down:** 10% decrease every 60 seconds (gradual scale-down for stability)
- **Stabilization:** 60s up, 300s down (prevents thrashing)

### 4. Ingress (`k8s/ingress.yaml`)

External access configuration for internal banking network:

#### Configuration
- **Ingress Class:** nginx
- **Host:** `transaction-processor.internal.banking.com` (matching CF route)
- **TLS:** Enabled with SSL redirect
- **Path Type:** Prefix
- **Backend Service:** `pe-eng-transaction-processor-dev:8080`

#### Security Features
- SSL redirect enforced
- TLS certificate: `transaction-processor-tls`
- Internal domain routing only

## Design Decisions

### Container Image Strategy
- **Registry:** `registry.bank.internal` (approved internal registry)
- **Tag:** `2.0.1` (semantic versioning, no `:latest`)
- **Provenance:** Follows Rule 03 for immutable, trusted images

### Resource Allocation
- **CPU Requests:** 500m (25% of limit) for guaranteed scheduling
- **CPU Limits:** 2000m (2 cores) for high-throughput processing
- **Memory:** 60% request-to-limit ratio following best practices
- **Total Cluster Resources:** 5 replicas × 500m CPU = 2.5 cores minimum

### Security Implementation
- **Pod Security:** Baseline compliance with all required settings
- **Network Security:** ClusterIP service limits internal access
- **Image Security:** Signed images from approved registry
- **Runtime Security:** Non-root execution with minimal capabilities

### High Availability
- **Replica Distribution:** 5 instances across cluster nodes
- **Health Monitoring:** Comprehensive liveness and readiness probes
- **Auto-scaling:** Dynamic scaling based on CPU and memory metrics
- **Circuit Breaker:** Hystrix integration for fault tolerance

### External Dependencies
- **Database Sharding:** 4 PostgreSQL instances with connection pooling
- **Message Streaming:** Kafka cluster with optimized consumer settings
- **Caching:** Redis cluster for performance optimization
- **Observability:** Audit service and metrics collector integration

## Deployment Strategy

### Rolling Update (Default)
- **Max Unavailable:** 25% (1-2 pods during updates)
- **Max Surge:** 25% (1-2 additional pods during updates)
- **Zero Downtime:** Service continues during deployments

### Validation Steps
1. **Syntax Validation:** `kubectl apply --dry-run=client -f k8s/`
2. **Resource Validation:** Verify ConfigMaps and Secrets exist
3. **Network Validation:** Test service discovery and ingress routing
4. **Health Validation:** Confirm probe endpoints respond correctly

## Migration Considerations

### From Cloud Foundry
- **Instance Count:** Maintained 5 instances for consistency
- **Memory Allocation:** Preserved 2GB limit from CF manifest
- **Service Bindings:** Mapped to Kubernetes ConfigMaps/Secrets
- **Health Checks:** Maintained 15-second timeout requirement
- **Environment Variables:** Direct translation from CF manifest

### Operational Changes
- **Scaling:** Manual CF scaling replaced with automatic HPA
- **Monitoring:** Enhanced Prometheus integration
- **Security:** Improved with Kubernetes security contexts
- **Networking:** Internal service mesh instead of CF routing

## Testing and Validation

### Local Testing with Kind
```bash
# Create local cluster
kind create cluster

# Apply manifests
kubectl apply -f k8s/

# Verify deployment
kubectl get pods,svc,hpa,ingress

# Check health
kubectl port-forward svc/pe-eng-transaction-processor-dev 8080:8080
curl http://localhost:8080/actuator/health

# Cleanup
kind delete cluster
```

### Production Readiness
- All manifests pass `kubectl apply --dry-run=client` validation
- Security contexts comply with Windsurf banking standards
- Resource limits prevent noisy neighbor issues
- Health probes ensure reliable service availability
- Auto-scaling handles variable transaction loads
