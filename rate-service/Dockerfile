FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="currency-converter-team"
LABEL service="rate-service"

VOLUME /tmp

EXPOSE 8080

WORKDIR /app

ARG SPRING_PROFILES_ACTIVE=development
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}

COPY target/rate-service-0.0.1-SNAPSHOT.jar app.jar

ENV TZ=UTC

RUN apk update && \
    apk upgrade && \
    apk add --no-cache libc6-compat gcompat && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && \
    rm -rf /var/cache/apk/*

ENTRYPOINT ["java","-Xmx512m","--add-opens","java.base/jdk.internal.misc=ALL-UNNAMED","--add-opens","java.base/sun.nio.ch=ALL-UNNAMED","-Djava.security.egd=file:/dev/./urandom","-jar","app.jar","--spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
