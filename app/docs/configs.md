# Default Configuration

## Networking Config

Overridable by `/etc/carbonio/tasks/config.properties`

| Key | Default |
| --- | ------- |
| `carbonio.postgresql.host` | `127.78.0.16` |
| `carbonio.postgresql.port` | `20000` |
| `carbonio.service-discover.host` | `127.0.0.1` |
| `carbonio.service-discover.port` | `8500` |
| `carbonio.service.host` | `0.0.0.0` |
| `carbonio.service.port` | `10000` |
| `carbonio.user-management.host` | `127.78.0.16` |
| `carbonio.user-management.port` | `20001` |

## Application Config

Overridable by Consul KV

| Key | Default | If not set |
| --- | ------- | ---------- |
| `carbonio-tasks/database/credentials/db-name` | *(not set)* | Crashes; but always set by database bootstrap |
| `carbonio-tasks/database/credentials/db-password` | *(not set)* | Crashes; but always set by database bootstrap |
| `carbonio-tasks/database/credentials/db-username` | *(not set)* | Crashes; but always set by database bootstrap |
| `carbonio-tasks/database/db-pool-idle-timeout` | *(not set)* | Quarkus default: 5 minutes |
| `carbonio-tasks/database/db-pool-leak-detection` | *(not set)* | Quarkus default: disabled |
| `carbonio-tasks/database/db-pool-max-lifetime` | *(not set)* | Quarkus default: no limit |
| `carbonio-tasks/database/db-pool-max-size` | *(not set)* | Quarkus default: 20 |
| `carbonio-tasks/database/db-pool-min-size` | *(not set)* | Quarkus default: 0 |
| `carbonio-tasks/db-name` | *(not set)* |  |
| `carbonio-tasks/db-password` | *(not set)* |  |
| `carbonio-tasks/db-username` | *(not set)* |  |
| `carbonio-tasks/hikari-idle-timeout` | *(not set)* |  |
| `carbonio-tasks/hikari-leak-detection-threshold` | *(not set)* |  |
| `carbonio-tasks/hikari-max-lifetime` | *(not set)* |  |
| `carbonio-tasks/hikari-max-pool-size` | *(not set)* |  |
| `carbonio-tasks/hikari-min-idle-connections` | *(not set)* |  |
| `carbonio-tasks/server/idle-timeout` | *(not set)* | Quarkus default: 30s |
| `carbonio-tasks/server/max-connections` | *(not set)* | Quarkus default: no limit |
| `carbonio-tasks/server/max-threads` | *(not set)* | Quarkus default: 200 |
| `carbonio-tasks/server/queue-size` | *(not set)* | Quarkus default: unbounded |

