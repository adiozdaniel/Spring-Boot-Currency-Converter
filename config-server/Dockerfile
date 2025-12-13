# Config Server Dockerfile
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="currency-converter-team"
LABEL service="config-server"
LABEL java.version="17"

VOLUME /tmp

EXPOSE 8888

WORKDIR /app

ARG SPRING_PROFILES_ACTIVE=development
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}

COPY target/config-server-0.0.1-SNAPSHOT.jar app.jar

ENV TZ=UTC

RUN apk update && \
    apk upgrade && \
    apk add --no-cache libc6-compat gcompat && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && \
    rm -rf /var/cache/apk/*

# JVM Configuration
# --add-opens: Required by Spring Cloud Config for encryption/decryption operations
# -Djava.security.egd: Use /dev/urandom to prevent entropy blocking in containers
ENTRYPOINT ["java", \
    "-Xmx512m", \
    "--add-opens", "java.base/java.lang=ALL-UNNAMED", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar", \
    "--spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
