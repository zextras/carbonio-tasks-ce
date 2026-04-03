// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.config;

import com.zextras.carbonio.quarkus.extensions.bootstrap.ConfigKey;

/**
 * Declares all networking and application config keys consumed by carbonio-tasks. These constants
 * are used with {@code NetworkingConfigService} and {@code ApplicationConfigService} at runtime and
 * are discovered by the build-time documentation generator via Jandex.
 */
public final class TasksServiceConfig {

  private TasksServiceConfig() {}

  public static final class NetworkingConfig {

    private NetworkingConfig() {}

    /** Host of the carbonio-user-management gRPC service. */
    @ConfigKey
    public static final String USER_MANAGEMENT_HOST = "carbonio.user-management.host";

    /** Port of the carbonio-user-management gRPC service. */
    @ConfigKey
    public static final String USER_MANAGEMENT_PORT = "carbonio.user-management.port";

    /** PostgreSQL host (must match the database extension's expected key). */
    @ConfigKey
    public static final String POSTGRES_HOST = "carbonio.postgresql.host";

    /** PostgreSQL port (must match the database extension's expected key). */
    @ConfigKey
    public static final String POSTGRES_PORT = "carbonio.postgresql.port";
  }

  public static final class ApplicationConfig {

    private ApplicationConfig() {}

    /** Database name fetched from Consul KV. */
    @ConfigKey
    public static final String DB_NAME = "db-name";

    /** Database username fetched from Consul KV. */
    @ConfigKey
    public static final String DB_USERNAME = "db-username";

    /** Database password fetched from Consul KV. */
    @ConfigKey
    public static final String DB_PASSWORD = "db-password";

    /** Hikari maximum pool size fetched from Consul KV. */
    @ConfigKey
    public static final String HIKARI_MAX_POOL_SIZE = "hikari-max-pool-size";

    /** Hikari minimum idle connections fetched from Consul KV. */
    @ConfigKey
    public static final String HIKARI_MIN_IDLE_CONNECTIONS = "hikari-min-idle-connections";

    /** Hikari idle timeout (ms) fetched from Consul KV. */
    @ConfigKey
    public static final String HIKARI_IDLE_TIMEOUT = "hikari-idle-timeout";

    /** Hikari leak detection threshold (ms) fetched from Consul KV. */
    @ConfigKey
    public static final String HIKARI_LEAK_DETECTION_THRESHOLD = "hikari-leak-detection-threshold";

    /** Hikari max lifetime (ms) fetched from Consul KV. */
    @ConfigKey
    public static final String HIKARI_MAX_LIFETIME = "hikari-max-lifetime";
  }
}
