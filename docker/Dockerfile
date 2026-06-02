# SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
#
# SPDX-License-Identifier: AGPL-3.0-only

# Stage 1 (BUILDPLATFORM): create users and select the arch-correct runner.
# Running on the build host arch means no QEMU needed for any RUN instruction.
# TARGETARCH is injected by buildah per platform (amd64 / arm64).
# dt3_pipeline places binaries at:
#   amd64 → app/target/*-runner
#   arm64 → app/target-arm64/*-runner
FROM --platform=$BUILDPLATFORM docker.io/library/ubuntu:noble AS prep
ARG TARGETARCH=amd64
RUN groupadd -r carbonio-tasks && useradd -r -g carbonio-tasks carbonio-tasks
# Copy the correct arch binary into a fixed location so the final COPY is
# architecture-agnostic (no shell expansion needed in the final stage).
COPY app/target/ /tmp/runner-amd64/
COPY app/target-arm64/ /tmp/runner-arm64/
RUN set -e; \
    if [ "${TARGETARCH}" = "arm64" ]; then \
        cp /tmp/runner-arm64/*-runner /tmp/runner; \
    else \
        cp /tmp/runner-amd64/*-runner /tmp/runner; \
    fi && chmod 755 /tmp/runner

# Stage 2 (TARGETPLATFORM): minimal runtime image, zero target-arch RUN commands.
FROM docker.io/library/ubuntu:noble
LABEL maintainer="Zextras <https://www.zextras.com>"
LABEL description="Carbonio Tasks CE - Task management service (native binary)"
WORKDIR /app
COPY --from=prep /etc/passwd /etc/shadow /etc/group /etc/
COPY --from=prep --chmod=755 /tmp/runner /app/carbonio-tasks-ce-runner
USER carbonio-tasks
EXPOSE 10000
ENTRYPOINT ["/app/carbonio-tasks-ce-runner", "-Djava.net.preferIPv4Stack=true"]
