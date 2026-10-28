# ---------- Etapa 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia só o pom primeiro: as dependências ficam em cache enquanto o pom não mudar
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Roda como usuário sem privilégios
RUN useradd --system --no-create-home appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
