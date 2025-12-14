# Rate Service Dockerfile
# Base image: Eclipse Temurin 17 JRE on Alpine Linux (minimal size)
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="currency-converter-team"
LABEL service="rate-service"
LABEL java.version="17"

# Temporary file storage location
VOLUME /tmp

# Service port
EXPOSE 8080

WORKDIR /app

# Build argument for Spring profile
ARG SPRING_PROFILES_ACTIVE=development
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}

# Copy the built JAR from target directory
# NOTE: JAR must be built before running docker build
COPY target/rate-service-0.0.1-SNAPSHOT.jar app.jar

# Set timezone to UTC
ENV TZ=UTC

# Install dependencies and configure timezone
RUN apk update && \
    apk upgrade && \
    apk add --no-cache libc6-compat gcompat && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && \
    rm -rf /var/cache/apk/*

# JVM Configuration
# --add-opens: Required by Netty for direct memory access and async NIO operations
# -Djava.security.egd: Use /dev/urandom to prevent entropy blocking in containers
# Spring Boot will automatically use SPRING_PROFILES_ACTIVE env var
ENTRYPOINT ["java", \
    "-Xmx512m", \
    "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED", \
    "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
