# -------------------------
# STAGE 1 — Build
# -------------------------
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copiamos solo pom primero (mejora cache)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiamos el resto del código
COPY src ./src

# Compilamos
RUN mvn clean package -DskipTests


# -------------------------
# STAGE 2 — Runtime
# -------------------------
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copiamos el jar generado desde el stage build
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
