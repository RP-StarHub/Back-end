FROM amazoncorretto:17-alpine-jdk
ARG JAR_FILE_PATH=build/libs/*.jar
ARG PROFILES
ARG ENV
COPY ${JAR_FILE_PATH} app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]