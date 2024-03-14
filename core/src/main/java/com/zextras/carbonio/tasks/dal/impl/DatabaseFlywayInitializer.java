// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal.impl;

import com.google.inject.Inject;
import com.zextras.carbonio.tasks.config.TasksConfig;
import com.zextras.carbonio.tasks.dal.DatabaseConnectionManager;
import com.zextras.carbonio.tasks.dal.DatabaseInitializer;
import com.zextras.carbonio.tasks.dal.repositories.DbInfoRepository;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseFlywayInitializer implements DatabaseInitializer {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseFlywayInitializer.class);

  private final TasksConfig tasksConfig;

  @Inject
  public DatabaseFlywayInitializer(TasksConfig tasksConfig) {
    this.tasksConfig = tasksConfig;
  }

  @Override
  public void initialize() {
    // Pre-condition: having the DatabaseConnectionManager object injected, we are sure that
    // the database already exists and the credentials are working properly.

    Flyway flyway = Flyway.configure()
            .dataSource(tasksConfig.getDataSource())
            .baselineOnMigrate(true) //if schema is not empty create baseline, if it is ignore
            .baselineVersion("0")
            .load();

    flyway.migrate();

  }
}
