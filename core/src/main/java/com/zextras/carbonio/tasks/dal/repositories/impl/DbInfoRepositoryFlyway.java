// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal.repositories.impl;

import com.google.inject.Inject;
import com.zextras.carbonio.tasks.Constants.Database.Tables;
import com.zextras.carbonio.tasks.config.TasksConfig;
import com.zextras.carbonio.tasks.dal.DatabaseConnectionManager;
import com.zextras.carbonio.tasks.dal.dao.DbInfo;
import com.zextras.carbonio.tasks.dal.repositories.DbInfoRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;

import java.sql.SQLException;

public class DbInfoRepositoryFlyway implements DbInfoRepository {

  private final TasksConfig tasksConfig;

  @Inject
  public DbInfoRepositoryFlyway(TasksConfig tasksConfig) {
    this.tasksConfig = tasksConfig;
  }

  @Override
  public String getDatabaseVersion() {
    Flyway flyway = Flyway.configure()
            .dataSource(tasksConfig.getDataSource())
            .load();

    return flyway.info().current() != null ? flyway.info().current().getVersion().getVersion() : "0";
  }

  @Override
  public boolean isDatabaseLive() {
    try{
      return tasksConfig.getDataSource().getConnection().isValid(5);
    }catch (SQLException e){
      return false;
    }
  }
}
