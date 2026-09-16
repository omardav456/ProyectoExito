# ---- Build stage ------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -B dependency:go-offline || true

COPY src src
RUN ./mvnw -B -DskipTests package

# ---- Runtime stage ----------------------------------------------------------
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd -r -u 1001 spring
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

COPY --from=build /app/target/*.jar app.jar
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]