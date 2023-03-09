// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.config;

import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.tasks.Constants.Config;
import com.zextras.carbonio.tasks.Constants.Config.Database;
import com.zextras.carbonio.tasks.Constants.Service;
import com.zextras.carbonio.tasks.Constants.ServiceDiscover.Config.Key;
import com.zextras.carbonio.tasks.clients.ServiceDiscoverHttpClient;
import com.zextras.carbonio.tasks.dal.dao.DbInfo;
import io.ebean.config.DatabaseConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TasksConfig {

  public String getDatabaseName() {
    return ServiceDiscoverHttpClient.defaultURL(Service.SERVICE_NAME)
        .getConfig(Key.DB_NAME)
        .orElse(Database.NAME);
  }

  public HikariDataSource getDataSource() {
    String databaseURL = System.getProperty(Config.Properties.DATABASE_URL);
    if (databaseURL == null) {
      databaseURL = Database.URL;
    }

    String databasePort = System.getProperty(Config.Properties.DATABASE_PORT);
    if (databasePort == null) {
      databasePort = Database.PORT;
    }

    String postgresUser =
        ServiceDiscoverHttpClient.defaultURL(Service.SERVICE_NAME)
            .getConfig(Key.DB_USERNAME)
            .orElse(Database.USERNAME);

    String postgresPassword =
        ServiceDiscoverHttpClient.defaultURL(Service.SERVICE_NAME)
            .getConfig(Key.DB_PASSWORD)
            .orElse("");

    String jdbcPostgresUrl =
        String.format("jdbc:postgresql://%s:%s/%s", databaseURL, databasePort, getDatabaseName());

    Properties dataSourceProperties = new Properties();
    dataSourceProperties.setProperty("sslmode", "disable");

    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(jdbcPostgresUrl);
    dataSource.setUsername(postgresUser);
    dataSource.setPassword(postgresPassword);
    dataSource.setDataSourceProperties(dataSourceProperties);
    return dataSource;
  }

  public DatabaseConfig getEbeanDatabaseConfig() {
    List<Class<?>> entityList = new ArrayList<>();
    entityList.add(DbInfo.class);

    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setName("carbonio-tasks-postgres");
    databaseConfig.setDataSource(getDataSource());
    databaseConfig.setDefaultServer(true);
    databaseConfig.addAll(entityList);

    return databaseConfig;
  }
}