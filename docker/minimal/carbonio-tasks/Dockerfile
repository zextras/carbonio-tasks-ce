FROM openjdk:17-jdk-slim

WORKDIR /app

RUN mkdir -p /etc/carbonio/tasks

COPY ../../boot/target/carbonio-tasks-*-jar-with-dependencies.jar .

CMD sh -c 'echo "\n\
carbonio.tasks.host=${CARBONIO_TASKS_HOST}\n\
carbonio.tasks.port=${CARBONIO_TASKS_PORT}\n\
\n\
carbonio.postgres.host=${CARBONIO_POSTGRES_HOST}\n\
carbonio.postgres.port=${CARBONIO_POSTGRES_PORT}\n\
\n\
carbonio.user-management.host=${CARBONIO_USER_MANAGEMENT_HOST}\n\
carbonio.user-management.port=${CARBONIO_USER_MANAGEMENT_PORT}" > /etc/carbonio/tasks/config.properties && \
JAR=$(ls carbonio-tasks-*-jar-with-dependencies.jar | head -n 1) && \
java -Djava.net.preferIPv4Stack=true \
     -Xms1024m \
     -Xmx2048m \
     -DTASKS_LOG_LEVEL=info \
     -jar "$JAR"'
