# Multi-stage build for LibroRed Spring Boot Application
# Stage 1: Build stage with Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy Maven files
COPY librored/backend/pom.xml ./
COPY librored/backend/src ./src/

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage with Java 21
FROM eclipse-temurin:21-jre-jammy

# Set working directory
WORKDIR /app

# Install curl for health checks and create non-root user for security
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/* && \
    addgroup --system librored && adduser --system --group librored

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown librored:librored app.jar

# Switch to non-root user
USER librored

# Expose the application port
EXPOSE 8443

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8443/actuator/health || exit 1

# Environment variables with defaults
ENV SPRING_PROFILES_ACTIVE=docker
ENV SERVER_PORT=8443
ENV MYSQL_HOST=mysql
ENV MYSQL_PORT=3306
ENV MYSQL_DATABASE=librored
ENV MYSQL_USERNAME=librored
ENV MYSQL_PASSWORD=librored123

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]