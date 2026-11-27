# SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
#
# SPDX-License-Identifier: AGPL-3.0-only

# native-image cannot cross-compile (oracle/graal#407): nativeBuild.aarch64 builds
# the arm64 runner on arm64 hardware and lands it in app/target-arm64/.
ARG SIDECAR_IMAGE=registry.dev.zextras.com/dev/carbonio-sidecar:main
FROM --platform=$BUILDPLATFORM docker.io/library/busybox AS runner

ARG TARGETARCH

COPY app/target/       /src/target/
COPY app/target-arm64/ /src/target-arm64/

RUN set -eu; mkdir -p /out; \
    case "${TARGETARCH}" in \
      amd64) src=/src/target ;; \
      arm64) src=/src/target-arm64 ;; \
      *) echo "unsupported TARGETARCH=${TARGETARCH}" >&2; exit 1 ;; \
    esac; \
    cp "${src}"/*-runner /out/carbonio-tasks-runner

FROM --platform=$BUILDPLATFORM docker.io/library/alpine:3.20 AS hcl
COPY package/carbonio-tasks-template.hcl /carbonio-tasks-template.hcl
RUN sed -E 's/\{\{ or \(env "[A-Z0-9_]+"\) "([^"]*)" \}\}/\1/g' \
    /carbonio-tasks-template.hcl > /carbonio-tasks.hcl \
    && ! grep -q '{{' /carbonio-tasks.hcl

FROM ${SIDECAR_IMAGE}

# Service registration
COPY --from=hcl /carbonio-tasks.hcl /consul/config/

# Setup files
COPY package/service-protocol.json /etc/carbonio/tasks/service-discover/
COPY package/intentions.json       /etc/carbonio/tasks/service-discover/
COPY package/policies.json         /etc/carbonio/tasks/service-discover/

# App binary (used for --setup only)
COPY --from=runner --chmod=755 /out/carbonio-tasks-runner /usr/share/carbonio/carbonio-tasks-ce-runner

# Setup script
COPY --chmod=755 docker/tasks-sidecar/carbonio-tasks-setup /usr/local/bin/carbonio-tasks-setup

ENV SERVICE_NAME=carbonio-tasks
ENV SETUP_SCRIPT="carbonio-tasks-setup"
ENV TOKEN_FILE=/etc/carbonio/tasks/service-discover/token
