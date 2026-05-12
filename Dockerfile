# ═══════════════════════════════════════════════════════════════════
# Stage 1: BUILD
# Uses Maven + JDK 17 image to compile and package the application.
# This stage is NOT included in the final image.
# ═══════════════════════════════════════════════════════════════════
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first – Docker caches dependencies when only source
# code changes (not pom.xml), so this layer is reused on rebuilds.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the fat JAR (skip tests; CI runs them)
COPY src ./src
RUN mvn package -DskipTests -B

# ═══════════════════════════════════════════════════════════════════
# Stage 2: RUN
# Slim Alpine JRE image (~85 MB). Only the JAR is copied here.
# No Maven, no JDK, no source code in production image.
# ═══════════════════════════════════════════════════════════════════
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# FIX: Alpine JRE images do not ship with wget by default.
# Install it so the HEALTHCHECK command works correctly.
RUN apk add --no-cache wget

# Copy only the executable JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose application port
EXPOSE 8080

# Health check – Docker marks the container unhealthy if this fails
HEALTHCHECK --interval=15s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
