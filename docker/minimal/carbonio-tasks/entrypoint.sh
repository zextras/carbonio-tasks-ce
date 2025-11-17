#!/bin/sh

echo "" > /etc/carbonio/tasks/config.properties

addEnvToProperties() {
  if [ -n "$2" ];
  then echo "$1=$2" >> /etc/carbonio/tasks/config.properties;
  else echo "$1 is not set. Skipping it.";
  fi
}

addEnvToProperties "carbonio.tasks.host" "${CARBONIO_TASKS_HOST}"
addEnvToProperties "carbonio.tasks.port" "${CARBONIO_TASKS_PORT}"

addEnvToProperties "carbonio.postgres.host" "${CARBONIO_POSTGRES_HOST}"
addEnvToProperties "carbonio.postgres.port" "${CARBONIO_POSTGRES_PORT}"

addEnvToProperties "carbonio.user-management.host" "${CARBONIO_USER_MANAGEMENT_HOST}"
addEnvToProperties "carbonio.user-management.port" "${CARBONIO_USER_MANAGEMENT_PORT}"

addEnvToProperties "carbonio.service-discover.host" "${CARBONIO_SERVICE_DISCOVER_HOST}"
addEnvToProperties "carbonio.service-discover.port" "${CARBONIO_SERVICE_DISCOVER_PORT}"


JAR=$(ls /app/carbonio-tasks-*-jar-with-dependencies.jar | head -n 1)

exec java -Djava.net.preferIPv4Stack=true \
          -Xms1024m \
          -Xmx2048m \
          -DTASKS_LOG_LEVEL=info \
          -jar "$JAR"
