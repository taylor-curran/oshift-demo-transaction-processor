FROM registry.access.redhat.com/ubi8/openjdk-17:latest

# Set working directory
WORKDIR /deployments

# Copy the JAR file
COPY target/transaction-processor-2.0.1.jar /deployments/app.jar

# Create non-root user for security
USER 1001

# Expose port 8080
EXPOSE 8080

# Set JVM options for production
ENV JAVA_OPTS="-Xmx1536m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/deployments/app.jar"]
