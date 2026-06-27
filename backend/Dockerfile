# ==============================
# STAGE 1 — Build the JAR
# ==============================
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests

# ==============================
# STAGE 2 — Run the JAR
# ==============================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 9090

ENTRYPOINT ["java","-jar","app.jar"]