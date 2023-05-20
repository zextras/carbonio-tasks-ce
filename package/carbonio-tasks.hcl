// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

services {
  check {
    http = "http://127.78.0.16:10000/rest/health/ready/",
    method = "GET",
    timeout = "1s"
    interval = "5s"
  }
  connect {
    sidecar_service {
      proxy {
        local_service_address = "127.78.0.16"
        upstreams = [
          {
            destination_name = "carbonio-tasks-db"
            local_bind_address = "127.78.0.16"
            local_bind_port = 20000
          },
          {
            destination_name = "carbonio-user-management"
            local_bind_address = "127.78.0.16"
            local_bind_port = 20001
          }
        ]
      }
    }
  }
  name = "carbonio-tasks"
  port = 10000
}
