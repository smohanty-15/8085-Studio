# ==============================
# STAGE 1 — Build the JAR
# ==============================
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Copy maven wrapper and pom first (better caching)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Make mvnw executable
RUN chmod +x ./mvnw

# Copy source code
COPY src ./src

# Build JAR — skip tests for speed
RUN ./mvnw clean package -DskipTests

# ==============================
# STAGE 2 — Run the JAR
# ==============================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy built JAR from stage 1 . 
COPY --from=build /app/target/*.jar app.jar

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]
