# Multi-stage build for Spring Boot application
FROM openjdk:25-jdk-slim AS build

# Set working directory
WORKDIR /app

# Install curl and unzip for Gradle installation
RUN apt-get update && apt-get install -y curl unzip && rm -rf /var/lib/apt/lists/*

# Install Gradle 9.1.0
RUN curl -L https://services.gradle.org/distributions/gradle-9.1.0-bin.zip -o gradle.zip \
    && unzip gradle.zip \
    && mv gradle-9.1.0 /opt/gradle \
    && rm gradle.zip

# Set Gradle environment
ENV GRADLE_HOME=/opt/gradle
ENV PATH=$PATH:$GRADLE_HOME/bin

# Copy gradle files first for better layer caching
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle gradle
COPY gradlew ./
RUN chmod +x gradlew

# Copy source code
COPY src src

# Build the application using the Gradle wrapper (preferred over system gradle)
RUN ./gradlew clean bootJar --no-daemon

# Runtime stage
FROM openjdk:25-jdk-slim

# Set working directory
WORKDIR /app

# Create logs directory
RUN mkdir -p /app/logs

# Copy the built jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
