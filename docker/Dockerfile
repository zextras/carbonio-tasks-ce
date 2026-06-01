# SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
#
# SPDX-License-Identifier: AGPL-3.0-only

# groupadd + useradd in a BUILDPLATFORM prep stage so the final image has no
# target-arch RUN (no QEMU => exec format error on arm64 cross-builds).
FROM --platform=$BUILDPLATFORM docker.io/library/ubuntu:noble AS prep
RUN groupadd -r carbonio-tasks && useradd -r -g carbonio-tasks carbonio-tasks

FROM docker.io/library/ubuntu:noble
LABEL maintainer="Zextras <https://www.zextras.com>"
LABEL description="Carbonio Tasks CE - Task management service (native binary)"

WORKDIR /app
# Users/groups from prep stage (same base image — identical /etc files)
COPY --from=prep /etc/passwd /etc/shadow /etc/group /etc/
# COPY --chmod avoids a target-arch RUN (QEMU-free)
COPY --chmod=755 app/target/*-runner /app/carbonio-tasks-ce-runner
USER carbonio-tasks
EXPOSE 10000
ENTRYPOINT ["/app/carbonio-tasks-ce-runner", "-Djava.net.preferIPv4Stack=true"]
