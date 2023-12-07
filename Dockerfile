FROM ghcr.io/navikt/baseimages/temurin:19

ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseParallelGC -XX:ActiveProcessorCount=2"

LABEL org.opencontainers.image.source=https://github.com/navikt/oppfolgingsplan-backend
COPY build/libs/*.jar app.jar
