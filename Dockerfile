# SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
#
# SPDX-License-Identifier: AGPL-3.0-only

FROM ubuntu:noble
WORKDIR /app
RUN groupadd -r carbonio-tasks && useradd -r -g carbonio-tasks carbonio-tasks
COPY app/target/*-runner ./runner
RUN chmod +x ./runner
USER carbonio-tasks
EXPOSE 10000
CMD ["./runner", "-Djava.net.preferIPv4Stack=true"]
