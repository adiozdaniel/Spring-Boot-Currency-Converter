FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="currency-converter-team"
LABEL service="main-service"

VOLUME /tmp

EXPOSE 8000

WORKDIR /app

ARG SPRING_PROFILES_ACTIVE=development
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}

COPY target/main-service-0.0.1-SNAPSHOT.jar app.jar

ENV TZ=UTC

RUN apk update && \
    apk upgrade && \
    apk add --no-cache libc6-compat gcompat && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && \
    rm -rf /var/cache/apk/*

# JVM Configuration
# --add-opens: Required by Netty for direct memory access and async NIO operations
# -Djava.security.egd: Use /dev/urandom to prevent entropy blocking in containers
ENTRYPOINT ["java", \
    "-Xmx512m", \
    "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED", \
    "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar", \
    "--spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
