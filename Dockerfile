FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

ARG JAR_FILE=target/media-task-platform-java-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
