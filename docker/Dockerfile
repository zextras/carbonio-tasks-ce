# SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
#
# SPDX-License-Identifier: AGPL-3.0-only

FROM --platform=linux/amd64 docker.io/library/ubuntu:noble

LABEL maintainer="Zextras <https://www.zextras.com>"
LABEL description="Carbonio Tasks CE - Task management service (native binary)"

RUN groupadd -r carbonio-tasks && useradd -r -g carbonio-tasks carbonio-tasks

WORKDIR /app

COPY app/target/*-runner /app/carbonio-tasks-ce-runner
RUN chmod +x /app/carbonio-tasks-ce-runner

USER carbonio-tasks

EXPOSE 10000

ENTRYPOINT ["/app/carbonio-tasks-ce-runner", "-Djava.net.preferIPv4Stack=true"]
