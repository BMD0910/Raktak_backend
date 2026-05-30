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

# Expose the default container port; Render overrides it through PORT.
EXPOSE 8080

# Start application
ENTRYPOINT ["sh", "-c", "exec java -jar /app/app.jar"]
