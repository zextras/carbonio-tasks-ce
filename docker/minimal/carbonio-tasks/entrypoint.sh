#!/bin/sh

cat > /etc/carbonio/tasks/config.properties <<EOF
carbonio.tasks.host=${CARBONIO_TASKS_HOST}
carbonio.tasks.port=${CARBONIO_TASKS_PORT}

carbonio.postgres.host=${CARBONIO_POSTGRES_HOST}
carbonio.postgres.port=${CARBONIO_POSTGRES_PORT}

carbonio.user-management.host=${CARBONIO_USER_MANAGEMENT_HOST}
carbonio.user-management.port=${CARBONIO_USER_MANAGEMENT_PORT}

carbonio.service-discover.host=${CARBONIO_SERVICE_DISCOVER_HOST}
carbonio.service-discover.port=${CARBONIO_SERVICE_DISCOVER_PORT}
EOF

JAR=$(ls /app/carbonio-tasks-*-jar-with-dependencies.jar | head -n 1)

exec java -Djava.net.preferIPv4Stack=true \
          -Xms1024m \
          -Xmx2048m \
          -DTASKS_LOG_LEVEL=info \
          -jar "$JAR"
