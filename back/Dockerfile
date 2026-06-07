# --- Build stage ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy pom first to leverage Docker layer caching
COPY back/pom.xml ./pom.xml


# Download dependencies (ignore tests)
# (no mvnw in repo, so we just run Maven from the base image)
# Maven isn't always available as a binary in the Render build container.
# Use the Maven wrapper if present; otherwise fall back to plain `mvn`.
# (Your repo currently has no mvnw, so `mvn` must exist in the base image.)
# If Maven isn't installed, install it.
# Eclipse Temurin Debian/Ubuntu-based images usually have apt-get.
RUN (command -v mvn >/dev/null 2>&1 && echo "mvn exists" ) || (apt-get update && apt-get install -y maven)

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

