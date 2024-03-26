// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal.impl;

import com.google.inject.Inject;
import com.zextras.carbonio.tasks.dal.DatabaseManager;
import com.zextras.carbonio.tasks.utilities.DatabaseUtils;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
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
    try {
      ResultSet flywayInfo = fetchFlywayInfo();
      return flywayInfo.getString("version");
    } catch (SQLException e) {
      return "0";
    }
  }

  @Override
  public boolean isDatabaseLive() {
    try {
      try (Connection connection = flyway.getConfiguration().getDataSource().getConnection()) {
        return connection.isValid(1);
      }
    } catch (SQLException e) {
      return false;
    }
  }

  @Override
  public boolean isDatabaseCorrectVersion() {
    // this returns if the last database version's script is included in
    // local migration path (db/migration). if the last version's script is not in path the database
    // was migrated
    // to a newer version by another client and this client must be updated.
    // this is to be sure that no other client has updated db and thus this instance doesn't have
    // the updated code
    // and is using old/no more existing tables

    // flyway.info() uses two connections at the same time and does not close them.
    // setting Constants.Hikari.MAX_POOL_SIZE to 3 instead
    // of 2 makes this call work; with MAX_POOL_SIZE = 2 this crashes with an InterruptException.
    // https://github.com/flyway/flyway/issues/3237

    // since I can't change max pool size here I manually implement "return
    // !flyway.info().current().getPhysicalLocation().isEmpty();"
    // using a single connection and closing it manually

    try (ResultSet flywayInfo = fetchFlywayInfo()) {
      String lastMigrationScript = flywayInfo.getString("script");

      // check if lastMigrationScript is in one of flyway locations
      Location[] locations =
          flyway
              .getConfiguration()
              .getLocations(); // this will return just one location, db/migration, but ynk
      for (Location location : locations) {
        if (DatabaseUtils.isScriptInPath(lastMigrationScript, location.getPath())) return true;
      }
      return false;
    } catch (SQLException e) {
      return false;
    }
  }

  private ResultSet fetchFlywayInfo() throws SQLException {
    try (Connection connection = flyway.getConfiguration().getDataSource().getConnection()) {
      Statement stm = connection.createStatement();
      ResultSet rs =
          stm.executeQuery(
              "select version, script "
                  + "from flyway_schema_history "
                  + "where success = true "
                  + "order by installed_on desc "
                  + "limit 1");

      rs.next(); // this query returns only one row and should not be empty
      return rs;
    }
  }
}
