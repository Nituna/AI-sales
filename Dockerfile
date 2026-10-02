# ========================================================
# Multi-Stage Dockerfile for AI Sales & Analytics System
# Render.com Web Service Deployment
# Base: Eclipse Temurin Java 21
# ========================================================

# --- Stage 1: Build the Application with Maven ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Cache Maven dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy project source and build shaded executable JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Stage 2: Minimal Production JRE Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Ensure SQLite storage directory exists
RUN mkdir -p /app/data

# Copy shaded runnable JAR from builder stage
COPY --from=builder /build/target/ai-sales-inventory-1.0.0.jar /app/app.jar

# Render assigns PORT dynamically; default to 8080 for local testing
ENV PORT=8080
EXPOSE 8080

# Run in headless mode and launch the embedded server
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "/app/app.jar"]
