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

FROM --platform=$BUILDPLATFORM docker.io/library/alpine:3.23 AS consul-template
ARG TARGETARCH
ARG CONSUL_TEMPLATE_VERSION=0.42.1
RUN apk add --no-cache curl unzip && \
    curl -fsSL -o /tmp/ct.zip "https://releases.hashicorp.com/consul-template/${CONSUL_TEMPLATE_VERSION}/consul-template_${CONSUL_TEMPLATE_VERSION}_linux_${TARGETARCH}.zip" && \
    curl -fsSL -o /tmp/ct.sums "https://releases.hashicorp.com/consul-template/${CONSUL_TEMPLATE_VERSION}/consul-template_${CONSUL_TEMPLATE_VERSION}_SHA256SUMS" && \
    echo "$(grep "_linux_${TARGETARCH}.zip" /tmp/ct.sums | cut -d' ' -f1)  /tmp/ct.zip" | sha256sum -c - && \
    unzip -j /tmp/ct.zip consul-template -d /out && \
    chmod +x /out/consul-template

FROM ${SIDECAR_IMAGE}

# Service registration
COPY --from=consul-template /out/consul-template /usr/local/bin/consul-template
COPY package/carbonio-tasks-template.hcl /consul/templates/carbonio-tasks-template.hcl

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

COPY --chmod=755 docker/tasks-sidecar/carbonio-tasks-hcl-entrypoint /usr/local/bin/carbonio-tasks-hcl-entrypoint
ENTRYPOINT ["carbonio-tasks-hcl-entrypoint"]
