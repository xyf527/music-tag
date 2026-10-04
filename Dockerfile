FROM --platform=linux/amd64 maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
# Private application.yml is excluded. Deployment configuration is supplied by environment.
RUN mvn -B -DskipTests package
FROM --platform=linux/amd64 eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* && groupadd --gid 10001 music && useradd --uid 10001 --gid music --create-home music
WORKDIR /app
COPY --from=build /build/target/music-tagger-*.jar /app/app.jar
USER 10001:10001
ARG GIT_COMMIT=UNKNOWN
ARG BUILD_TIME=UNKNOWN
ARG BUILD_VERSION=UNKNOWN
ENV MUSIC_BUILD_COMMIT=$GIT_COMMIT MUSIC_BUILD_TIME=$BUILD_TIME MUSIC_BUILD_VERSION=$BUILD_VERSION
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
