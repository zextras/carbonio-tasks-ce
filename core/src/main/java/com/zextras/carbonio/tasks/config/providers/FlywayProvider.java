// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.config.providers;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.tasks.config.TasksConfig;
import org.flywaydb.core.Flyway;

public class FlywayProvider implements Provider<Flyway> {
  private final TasksConfig tasksConfig;

  @Inject
  public FlywayProvider(TasksConfig tasksConfig) {
    this.tasksConfig = tasksConfig;
  }

  @Override
  public Flyway get() {
    HikariDataSource dataSource = tasksConfig.getDataSource();
    return Flyway.configure()
        .dataSource(dataSource.getJdbcUrl(), dataSource.getUsername(), dataSource.getPassword())
        .baselineOnMigrate(true) // if schema is not empty create baseline, if it is ignored
        .baselineVersion("0")
        .load();
  }
}
