# Build stage
FROM eclipse-temurin:17-jdk-jammy as builder

WORKDIR /app

# Copy project files
COPY . .

# Build JAR (skip tests for faster build in CI/CD)
RUN ./mvnw -DskipTests=true package

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Set production profile by default
ENV SPRING_PROFILES_ACTIVE=prod

# Expose port (Railway will replace $PORT in start command)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

# Start application
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
