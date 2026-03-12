# Auth Service Dockerfile
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="currency-converter-team"
LABEL service="auth-service"
LABEL java.version="17"

VOLUME /tmp

EXPOSE 8081

WORKDIR /app

ARG SPRING_PROFILES_ACTIVE=development
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}

COPY target/*.jar app.jar

ENV TZ=UTC

RUN apk update && \
    apk upgrade && \
    apk add --no-cache libc6-compat gcompat && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && \
    rm -rf /var/cache/apk/*

# JVM Configuration
# -Djava.security.egd: Use /dev/urandom to prevent entropy blocking in containers
# Spring Boot will automatically use SPRING_PROFILES_ACTIVE env var
ENTRYPOINT ["java", \
    "-Xmx512m", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
