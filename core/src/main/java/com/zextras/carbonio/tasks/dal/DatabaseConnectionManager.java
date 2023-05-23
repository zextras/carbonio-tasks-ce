// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.tasks.config.TasksConfig;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the creation of a single instance of the Ebean {@link Database} to execute sql query. It
 * must be a {@link Singleton} since the system must have only one instance of the {@link Database}
 * for the entire execution.
 */
@Singleton
public class DatabaseConnectionManager {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionManager.class);

  private final TasksConfig tasksConfig;
  private Database ebeanDatabase;

  @Inject
  public DatabaseConnectionManager(TasksConfig tasksConfig) {
    this.tasksConfig = tasksConfig;
  }

  /**
   * Retrieves the {@link DatabaseConfig} containing the {@link DataSource} from the {@link
   * TasksConfig} class, then it tries to create the connection to the database.
   *
   * @return a {@link Database} connection necessary to perform Ebean sql operations.
   * @throws {@link RuntimeException} if the connection creation to the database fails.
   */
  public Database getEbeanDatabase() {
    if (ebeanDatabase == null) {
      try {
        ebeanDatabase =
            DatabaseFactory.createWithContextClassLoader(
                tasksConfig.getEbeanDatabaseConfig(), TasksConfig.class.getClassLoader());
      } catch (Exception exception) {
        String error =
            String.format(
                "%s: e.g. %s, %s or %s",
                "Unable to create the database connection! Something went wrong",
                "database is not reachable",
                "the database does not exist",
                "the database credentials are wrong");

        throw new RuntimeException(error, exception);
      }

      logger.info("Database connection created successfully");
    }

    return ebeanDatabase;
  }
}
