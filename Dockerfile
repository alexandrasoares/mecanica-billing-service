# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests -Djacoco.skip=true package

FROM eclipse-temurin:17-jre-jammy

RUN groupadd --system billing && useradd --system --gid billing billing

WORKDIR /app
COPY --from=build /app/target/mecanica-billing-service-0.0.1-SNAPSHOT.jar app.jar

USER billing
EXPOSE 8080

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
