FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

RUN mkdir -p /etc/carbonio/tasks

COPY ../../app/target/carbonio-tasks-ce-app-*-runner.jar app.jar
COPY docker/minimal/carbonio-tasks/entrypoint.sh entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]