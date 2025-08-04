FROM registry.access.redhat.com/ubi8/openjdk-17:latest

# Set working directory
WORKDIR /deployments

# Copy the JAR file
COPY target/transaction-processor-2.0.1.jar /deployments/app.jar

# UBI8 OpenJDK image already runs as non-root user (jboss:0)
# Ensure proper permissions for the application
USER root
RUN chown -R jboss:0 /deployments && chmod -R g+rw /deployments
USER jboss

# Expose port
EXPOSE 8080

# Entry point
ENTRYPOINT ["java", "-jar", "/deployments/app.jar"]
