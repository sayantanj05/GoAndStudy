# --- Build stage ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy pom first to leverage Docker layer caching
COPY pom.xml ./

# Download dependencies (ignore tests)
# (no mvnw in repo, so we just run Maven from the base image)
RUN mvn -q -DskipTests dependency:go-offline || mvn -q -DskipTests dependency:resolve


# Copy source
COPY src ./src

# Build jar
RUN mvn -q -DskipTests package

# --- Runtime stage ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar
COPY --from=build /app/target/*.jar ./app.jar

EXPOSE 8080

# Render sets PORT env var; Spring already uses ${PORT:8080}
ENV SERVER_PORT=8080

ENTRYPOINT ["sh", "-c", "java -jar /app/app.jar --server.port=${PORT:-8080}"]

