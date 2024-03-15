// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal.impl;

import com.google.inject.Inject;
import com.zextras.carbonio.tasks.dal.DatabaseManager;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

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
    return flyway.info().current() != null ? flyway.info().current().getVersion().getVersion() : "0";
  }

  @Override
  public boolean isDatabaseLive() {
    try{
      return flyway.getConfiguration().getDataSource().getConnection().isValid(1);
    }catch (SQLException e){
      return false;
    }
  }

  @Override
  public boolean isDatabaseCorrectVersion() {
    //flyway resolver will return an empty physical path if the last database version's script is not included in
    //local migration path (db/migration). if the last version's script is not in path the database was migrated
    //to a newer version by another client and this client must be updated
    //this is to be sure that no other client has updated db and thus this instance doesn't have the updated code
    //and is using old/no more existing tables
    return !flyway.info().current().getPhysicalLocation().isEmpty();
  }
}
