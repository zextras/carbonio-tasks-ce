// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.config;

import com.zextras.carbonio.tasks.Constants;
import com.zextras.carbonio.tasks.Constants.Config.Database;
import com.zextras.carbonio.tasks.Constants.Config.Hikari;
import com.zextras.carbonio.tasks.Constants.Config.ServiceDiscover;
import com.zextras.carbonio.tasks.Constants.Tasks;
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

  private TasksConfig(Properties properties) {
    this.properties = properties;
  }

  public static TasksConfig getConfig() {
    final Properties properties = new Properties();
    loadFromEtc() // the official way
        .ifPresent(config -> {
          try {
            properties.load(config);
          } catch (IOException e) {
            logger.warn("Error loading configuration file: {}", e.getMessage());
          }
        });
    properties.putAll(System.getProperties());
    return new TasksConfig(properties);
  }

  private static Optional<InputStream> loadFromEtc() {
    return loadFile("/etc/carbonio/tasks/config.properties");
  }

  private static Optional<InputStream> loadFile(String path) {
    try {
      return Optional.of(new FileInputStream(path));
    } catch (FileNotFoundException e) {
      return Optional.empty();
    }
  }

  public String getDatabaseHost() {
    return properties.getProperty(
        Database.HOST_PROPERTY,
        Database.DEFAULT_HOST);
  }

  public String getDatabasePort() {
    return properties.getProperty(
        Database.PORT_PROPERTY,
        Database.DEFAULT_PORT);
  }

  public String getDatabaseName() {
    return getConfig(Key.DB_NAME)
        .orElse(Database.DEFAULT_NAME);
  }

  public String getDatabaseUsername() {
    return getConfig(Key.DB_USERNAME)
        .orElse(Database.DEFAULT_USERNAME);
  }

  public String getDatabasePassword() {
    return getConfig(Key.DB_PASSWORD)
        .orElse("");
  }

  public String getTasksHost() {
    return properties.getProperty(
        Constants.Tasks.HOST_PROPERTY,
        Constants.Tasks.DEFAULT_HOST);
  }

  public String getTasksPort() {
    return properties.getProperty(
        Constants.Tasks.PORT_PROPERTY,
        String.valueOf(Constants.Tasks.DEFAULT_PORT));
  }

  public String getUserManagementHost() {
    return properties.getProperty(
        Constants.Config.UserManagement.HOST_PROPERTY,
        Constants.Config.UserManagement.DEFAULT_HOST);
  }

  public String getUserManagementPort() {
    return properties.getProperty(
        Constants.Config.UserManagement.PORT_PROPERTY,
        String.valueOf(Constants.Config.UserManagement.DEFAULT_PORT));
  }

  public int getHikariMaxPoolSize() {
    return getConfigInt(Key.HIKARI_MAX_POOL_SIZE)
        .orElse(Hikari.MAX_POOL_SIZE);
  }

  public int getHikariMinIdleConnections() {
    int maxPoolSize = getHikariMaxPoolSize();
    return getConfigInt(Key.HIKARI_MIN_IDLE_CONNECTIONS)
        .map(minIdleConnections -> Math.min(minIdleConnections, maxPoolSize))
        .orElse(Hikari.MIN_IDLE_CONNECTIONS);
  }

  public int getHikariIdleTimeout() {
    return getConfigInt(Key.HIKARI_IDLE_TIMEOUT)
      .orElse(Hikari.IDLE_TIMEOUT);
  }

  public int getHikariLeakDetectionThreshold() {
    return getConfigInt(Key.HIKARI_LEAK_DETECTION_THRESHOLD)
      .orElse(Hikari.LEAK_DETECTION_THRESHOLD);
  }

  public int getHikariMaxLifetime() {
    return getConfigInt(Key.HIKARI_MAX_LIFETIME)
      .orElse(Hikari.MAX_LIFETIME);
  }

  private Optional<Integer> getConfigInt(String key) {
    return getConfig(key)
      .map(Integer::parseInt);
  }

  public String getServiceDiscoverEndpoint() {
    return "http://" + properties.getProperty(
        ServiceDiscover.HOST_PROPERTY,
        ServiceDiscover.DEFAULT_HOST) + ":" + properties.getProperty(ServiceDiscover.PORT_PROPERTY,
				String.valueOf(ServiceDiscover.DEFAULT_PORT));
  }

  private Optional<String> getConfig(String key) {
    return ServiceDiscoverHttpClient.atURL(this.getServiceDiscoverEndpoint(), Tasks.SERVICE_NAME)
      .getConfig(key);
  }
}
