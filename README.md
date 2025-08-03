# Transaction Processor

## Artifact Design Thinking

**Platform**: Traditional Cloud Foundry  
**Complexity**: High

### Design Rationale
This represents a high-throughput transaction processing engine for banking operations. The artifacts demonstrate:

- **High-performance configuration** with 5 instances and 2GB memory per instance
- **Database sharding** across 4 separate database instances for scalability
- **Kafka streaming** for real-time transaction processing with optimized consumer settings
- **JVM performance tuning** with G1 garbage collector and specific heap settings
- **Circuit breaker patterns** (Hystrix) for system resilience
- **Enterprise health checks** with extended timeout for complex processing

### Key Complexity Features
- Multi-shard database architecture for horizontal scaling
- High-throughput Kafka consumer configuration
- Circuit breaker and retry patterns
- Batch processing optimization (1000 record batches)
- Comprehensive service dependencies (audit, metrics, multiple DBs)

## Running and Testing

### Prerequisites
- Java 17 (required by Spring Boot 2.7.8)
- Maven 3.6+

### Environment Setup
```bash
# Ensure Java 17 is installed and set as default
java -version  # Should show version 17.x.x

# If using SDKMAN
sdk install java 17-open
sdk use java 17-open
```

### Build and Test
```bash
# Install dependencies
mvn clean install

# Run tests
mvn test

# Build application
mvn clean package

# Run locally (requires Kafka and PostgreSQL configuration)
mvn spring-boot:run
```

### Test Configuration
The application includes a basic test that verifies the Spring context loads correctly. Tests use an in-memory H2 database and mock Kafka configuration for isolated testing.

### Cloud Foundry Deployment
```bash
cf push
```
