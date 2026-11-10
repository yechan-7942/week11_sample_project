FROM gradle:8.14.3-jdk17 AS build
WORKDIR /workspace

COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle --no-daemon bootJar \
    && cp build/libs/*.jar /workspace/app.jar

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd --system spring \
    && useradd --system --gid spring spring \
    && mkdir /app/uploads \
    && chown spring:spring /app/uploads

COPY --from=build --chown=spring:spring /workspace/app.jar ./app.jar

USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
