// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.config.migration;

import com.zextras.carbonio.quarkus.extensions.bootstrap.setup.migration.ConfigMigration;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Migrates the DB-credential KV keys that early carbonio-tasks-db releases wrote in DOTTED form
 * ({@code carbonio-tasks/database.credentials.db-*}) to the documented nested SLASH path ({@code
 * carbonio-tasks/database/credentials/db-*}).
 *
 * <p>The documented contract (configs.md), {@link V1__RenameDbCredentials}, and the fixed
 * carbonio-tasks-db bootstrap all use the slash path. The carbonio-quarkus-extensions reader maps
 * that path to the property {@code application-config.database.credentials.db-name} via {@code
 * replace('/','.')}, so the dotted key happens to resolve to the same property at runtime — but it
 * does not match the documented layout, and a path-based (non-extension) KV consumer reading {@code
 * carbonio-tasks/database/credentials/db-name} would miss it.
 *
 * <p>This migration only affects instances that actually carry the dotted keys, i.e. fresh installs
 * created by a tasks-db release that wrote them. Upgraded instances already have the slash path
 * (via {@link V1__RenameDbCredentials}) and future fresh installs get it directly (fixed
 * bootstrap), so here the dotted old key is absent and the entry is skipped. Value-preserving (no
 * password regeneration): the runner reads the existing dotted value, writes it to the slash key,
 * then deletes the dotted key. Idempotent: a re-run finds no dotted key and is a no-op.
 */
public class V2__MigrateDottedDbCredentialsToSlash extends ConfigMigration {

  private static final String SVC = "carbonio-tasks";

  @Override
  protected Map<String, BiConsumer<String, String>> networkingMigrations() {
    return Map.of();
  }

  @Override
  protected Map<String, BiConsumer<String, String>> applicationMigrations() {
    return Map.of(
        SVC + "/database.credentials.db-name",
        (k, v) -> applicationConfig.set(SVC + "/database/credentials/db-name", v),
        SVC + "/database.credentials.db-username",
        (k, v) -> applicationConfig.set(SVC + "/database/credentials/db-username", v),
        SVC + "/database.credentials.db-password",
        (k, v) -> applicationConfig.set(SVC + "/database/credentials/db-password", v));
  }
}
