// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.config;

import com.zextras.carbonio.quarkus.extensions.bootstrap.ConfigKey;

/**
 * Declares all networking config keys consumed by carbonio-tasks. These constants are used with
 * {@code NetworkingConfigService} at runtime and are discovered by the build-time documentation
 * generator via Jandex.
 *
 * <p>Application (Consul KV) config keys are defined in {@code
 * CarbonioDatabaseServiceConfig.ApplicationConfig} from the database extension.
 */
public final class TasksServiceConfig {

  private TasksServiceConfig() {}

  public static final class NetworkingConfig {

    private NetworkingConfig() {}

    /** Host of the carbonio-user-management REST service. */
    @ConfigKey public static final String USER_MANAGEMENT_HOST = "carbonio.user-management.host";

    /** Port of the carbonio-user-management REST service. */
    @ConfigKey public static final String USER_MANAGEMENT_PORT = "carbonio.user-management.port";

    /** PostgreSQL host (must match the database extension's expected key). */
    @ConfigKey public static final String POSTGRES_HOST = "carbonio.postgresql.host";

    /** PostgreSQL port (must match the database extension's expected key). */
    @ConfigKey public static final String POSTGRES_PORT = "carbonio.postgresql.port";
  }
}
