// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.config;

import com.zextras.carbonio.tasks.Constants;
import com.zextras.carbonio.tasks.Constants.Config.Database;
import com.zextras.carbonio.tasks.Constants.Config.Hikari;
import com.zextras.carbonio.tasks.Constants.Service;
import com.zextras.carbonio.tasks.Constants.ServiceDiscover.Config.Key;
import com.zextras.carbonio.tasks.clients.ServiceDiscoverHttpClient;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TasksConfig {

  private static final Logger logger = LoggerFactory.getLogger(TasksConfig.class);

  private final Properties properties;

  public TasksConfig() {
    properties = new Properties();
  }

  // Load config from files or system properties.
  public void loadConfig() throws IOException {
    loadFromEtc() // the official way
      .or(this::loadFromCurrent) // the fallback way
      .or(this::loadFromResources) // the last resort way
      .ifPresent(config -> {
        try {
          properties.load(config);
        } catch (IOException e) {
          logger.warn("Error loading configuration file: {}", e.getMessage());
        }
      });

    properties.putAll(System.getProperties()); // the dev way, overriding existing properties
  }

  private Optional<InputStream> loadFromEtc() {
    return loadFile("/etc/carbonio/tasks/config.properties");
  }

  private Optional<InputStream> loadFromCurrent() {
    return loadFile("resources/carbonio-tasks.properties");
  }

  private Optional<InputStream> loadFromResources() {
    return Optional.ofNullable(
      getClass().getClassLoader().getResourceAsStream("carbonio-tasks.properties"));
  }

  private Optional<InputStream> loadFile(String path) {
    try {
      return Optional.of(new FileInputStream(path));
    } catch (FileNotFoundException e) {
      return Optional.empty();
    }
  }

  public Properties getProperties() {
    return properties;
  }

  public String getDatabaseUrl() {
    return properties.getProperty(
        Constants.Config.Properties.DATABASE_URL,
        Database.URL);
  }

  public String getDatabasePort() {
    return properties.getProperty(
        Constants.Config.Properties.DATABASE_PORT,
        Database.PORT);
  }

  public String getDatabaseName() {
    return ServiceDiscoverHttpClient.defaultURL(Service.SERVICE_NAME)
        .getConfig(Key.DB_NAME)
        .orElse(Database.NAME);
  }

  public String getDatabaseUsername() {
    return ServiceDiscoverHttpClient.defaultURL(Service.SERVICE_NAME)
        .getConfig(Key.DB_USERNAME)
        .orElse(Database.USERNAME);
  }

  public String getDatabasePassword() {
    return ServiceDiscoverHttpClient.defaultURL(Service.SERVICE_NAME)
        .getConfig(Key.DB_PASSWORD)
        .orElse("");
  }

  public int getHikariMaxPoolSize() {
    return ServiceDiscoverHttpClient.defaultURL(Service.SERVICE_NAME)
        .getConfig(Key.HIKARI_MAX_POOL_SIZE)
        .map(Integer::parseInt)
        .orElse(Hikari.MAX_POOL_SIZE);
  }

  public int getHikariMinIdleConnections() {
    int maxPoolSize = getHikariMaxPoolSize();
    return ServiceDiscoverHttpClient.defaultURL(Service.SERVICE_NAME)
        .getConfig(Key.HIKARI_MIN_IDLE_CONNECTIONS)
        .map(minIdleConnections ->
            Math.min(Integer.parseInt(minIdleConnections), maxPoolSize))
        .orElse(Hikari.MIN_IDLE_CONNECTIONS);
  }
}
