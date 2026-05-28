FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

RUN chmod +x gradlew \
    && ./gradlew bootJar --no-daemon \
    && cp build/libs/*.jar app.jar

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/app.jar ./app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
