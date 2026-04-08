// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.config.migration;

import com.zextras.carbonio.quarkus.extensions.bootstrap.setup.migration.ConfigMigration;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Migrates Consul KV keys from the pre-Quarkus naming (hikari-*, db-*) to the
 * carbonio-quarkus-extensions-database naming (database.credentials.*, database.db-pool-*).
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
        (k, v) -> applicationConfig.set(SVC + "/database.credentials.db-name", v),
        SVC + "/db-username",
        (k, v) -> applicationConfig.set(SVC + "/database.credentials.db-username", v),
        SVC + "/db-password",
        (k, v) -> applicationConfig.set(SVC + "/database.credentials.db-password", v),
        SVC + "/hikari-max-pool-size",
        (k, v) -> applicationConfig.set(SVC + "/database.db-pool-max-size", v),
        SVC + "/hikari-min-idle-connections",
        (k, v) -> applicationConfig.set(SVC + "/database.db-pool-min-size", v),
        SVC + "/hikari-idle-timeout",
        (k, v) -> applicationConfig.set(SVC + "/database.db-pool-idle-timeout", v),
        SVC + "/hikari-leak-detection-threshold",
        (k, v) -> applicationConfig.set(SVC + "/database.db-pool-leak-detection", v),
        SVC + "/hikari-max-lifetime",
        (k, v) -> applicationConfig.set(SVC + "/database.db-pool-max-lifetime", v));
  }
}
