# SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
#
# SPDX-License-Identifier: AGPL-3.0-only

FROM eclipse-temurin:21.0.10_7-jdk-alpine

WORKDIR /app

RUN mkdir -p /etc/carbonio/tasks

COPY ../../app/target/carbonio-tasks-ce-app-*-runner.jar app.jar
COPY docker/minimal/carbonio-tasks/entrypoint.sh entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]