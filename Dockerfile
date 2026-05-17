# Build stage using Maven official image (contains mvn)
FROM maven:3.9.6-eclipse-temurin-17 as builder

WORKDIR /app

# Copy project files
COPY . .

# Build JAR (skip tests for faster CI/CD builds)
RUN mvn -B -DskipTests package

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Set production profile by default
ENV SPRING_PROFILES_ACTIVE=prod

# Expose port (Railway will replace $PORT in start command)
EXPOSE 8080

# Health check (requires curl in runtime; fallback to simple java process check if absent)
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD ["/bin/sh","-c","(command -v curl >/dev/null 2>&1 && curl -f http://localhost:${PORT:-8080}/actuator/health) || (netstat -tnlp 2>/dev/null | grep -q java) || exit 1"]

# Start application
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
