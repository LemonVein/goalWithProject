FROM amazoncorretto:17

ARG JAR_FILE=build/libs/*.jar

COPY ${JAR_FILE} app.jar

LABEL authors="jasonkim"

ENTRYPOINT ["java", "-jar", "/app.jar"]