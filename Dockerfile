FROM openjdk:21-jre-slim

WORKDIR /app

COPY build/libs/backup-orchestrator-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8989

ENTRYPOINT ["java", "-jar", "app.jar"]