FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

RUN mkdir -p /etc/carbonio/tasks

COPY ../../boot/target/carbonio-tasks-*-jar-with-dependencies.jar .
COPY docker/minimal/carbonio-tasks/entrypoint.sh entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]