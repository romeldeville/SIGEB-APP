# Stage 1 : Build Maven
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

# Stage 2 : Runtime léger (utilisateur non-root)
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S sigeb && adduser -S sigeb -G sigeb
COPY --from=build /app/target/*.jar /app/sigeb.jar
USER sigeb
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/sigeb.jar"]
