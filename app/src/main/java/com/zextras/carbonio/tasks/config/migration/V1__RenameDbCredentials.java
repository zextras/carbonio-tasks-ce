// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.config.migration;

import com.zextras.carbonio.quarkus.extensions.bootstrap.setup.migration.ConfigMigration;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Migrates Consul KV keys from the pre-Quarkus naming (hikari-*, db-*) to the
 * carbonio-quarkus-extensions-database slash-separated KV paths.
 *
 * <p>ConsulKvClient.set(key, value) constructs the URL as {consulBaseUrl}/v1/kv/{key}, so target
 * keys must use Consul KV path separators (slashes), not property name separators (dots). The
 * database extension reads from carbonio-tasks/database/credentials/db-name (slashes) which
 * SmallRye Config maps to the property application-config.database.credentials.db-name (dots).
 *
 * <p>Idempotent: each entry is skipped if the old key no longer exists in Consul.
 */
public class V1__RenameDbCredentials extends ConfigMigration {

  private static final String SVC = "carbonio-tasks";

  @Override
  protected Map<String, BiConsumer<String, String>> networkingMigrations() {
    return Map.of();
  }

  @Override
  protected Map<String, BiConsumer<String, String>> applicationMigrations() {
    return Map.of(
        SVC + "/db-name",
        (k, v) -> applicationConfig.set(SVC + "/database/credentials/db-name", v),
        SVC + "/db-username",
        (k, v) -> applicationConfig.set(SVC + "/database/credentials/db-username", v),
        SVC + "/db-password",
        (k, v) -> applicationConfig.set(SVC + "/database/credentials/db-password", v),
        SVC + "/hikari-max-pool-size",
        (k, v) -> applicationConfig.set(SVC + "/database/db-pool-max-size", v),
        SVC + "/hikari-min-idle-connections",
        (k, v) -> applicationConfig.set(SVC + "/database/db-pool-min-size", v),
        SVC + "/hikari-idle-timeout",
        (k, v) -> applicationConfig.set(SVC + "/database/db-pool-idle-timeout", v),
        SVC + "/hikari-leak-detection-threshold",
        (k, v) -> applicationConfig.set(SVC + "/database/db-pool-leak-detection", v),
        SVC + "/hikari-max-lifetime",
        (k, v) -> applicationConfig.set(SVC + "/database/db-pool-max-lifetime", v));
  }
}
