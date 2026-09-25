# SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
#
# SPDX-License-Identifier: AGPL-3.0-only

# CE builds only the amd64 native runner, so pin the platform like docker/Dockerfile.
FROM --platform=linux/amd64 registry.dev.zextras.com/dev/carbonio-sidecar:devel

# Service registration
COPY package/carbonio-tasks.hcl /consul/config/

# Setup files
COPY package/service-protocol.json /etc/carbonio/tasks/service-discover/
COPY package/intentions.json       /etc/carbonio/tasks/service-discover/
COPY package/policies.json         /etc/carbonio/tasks/service-discover/

# App binary (used for --setup only)
COPY --chmod=755 app/target/*-runner /usr/share/carbonio/carbonio-tasks-ce-runner

# Setup script
COPY --chmod=755 docker/tasks-sidecar/carbonio-tasks-setup /usr/local/bin/carbonio-tasks-setup

ENV SERVICE_NAME=carbonio-tasks
ENV SETUP_SCRIPT="carbonio-tasks-setup"
ENV TOKEN_FILE=/etc/carbonio/tasks/service-discover/token
