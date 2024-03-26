// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal.impl;

import com.google.inject.Inject;
import com.zextras.carbonio.tasks.dal.DatabaseManager;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseManagerFlyway implements DatabaseManager {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseManagerFlyway.class);

  private final Flyway flyway;

  @Inject
  public DatabaseManagerFlyway(Flyway flyway) {
    this.flyway = flyway;
  }

  @Override
  public void initialize() {
    // Pre-condition: having the DatabaseConnectionManager object injected, we are sure that
    // the database already exists and the credentials are working properly.
    flyway.migrate();
  }

  @Override
  public String getDatabaseVersion() {
    MigrationInfo current = flyway.info().current();
    return current != null ? flyway.info().current().getVersion().getVersion() : "0";
  }

  @Override
  public boolean isDatabaseLive() {
    try {
      return flyway.getConfiguration().getDataSource().getConnection().isValid(1);
    } catch (SQLException e) {
      return false;
    }
  }

  @Override
  public boolean isDatabaseCorrectVersion() {
    return !flyway.info().current().getPhysicalLocation().isEmpty();
  }
}
