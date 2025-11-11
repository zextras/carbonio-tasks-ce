// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.tasks.Constants;
import com.zextras.carbonio.tasks.Constants.Tasks.API.Endpoints;
import com.zextras.carbonio.tasks.auth.AuthenticationServletFilter;
import com.zextras.carbonio.tasks.dal.DatabaseManager;
import com.zextras.carbonio.tasks.dal.dao.Task;
import com.zextras.carbonio.tasks.dal.impl.DatabaseManagerFlyway;
import com.zextras.carbonio.tasks.dal.repositories.TaskRepository;
import com.zextras.carbonio.tasks.dal.repositories.impl.TaskRepositoryEbean;
import com.zextras.carbonio.tasks.graphql.GraphQLServlet;
import com.zextras.carbonio.tasks.rest.RestApplication;
import com.zextras.carbonio.tasks.rest.controllers.HealthController;
import com.zextras.carbonio.tasks.rest.controllers.HealthControllerImpl;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import org.flywaydb.core.Flyway;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class TasksModule extends AbstractModule {

  private static final Logger logger = LoggerFactory.getLogger(TasksModule.class);

  @Override
  protected void configure() {
    // Basic bindings
    bind(Clock.class).toInstance(Clock.systemUTC());
    bind(HealthController.class).to(HealthControllerImpl.class);
    bind(TaskRepository.class).to(TaskRepositoryEbean.class);
    bind(DatabaseManager.class).to(DatabaseManagerFlyway.class);

    // Servlet configuration
    install(
        new ServletModule() {
          @Override
          protected void configureServlets() {
            bind(ResteasyJackson2Provider.class);
            bind(GraphQLServlet.class).in(Singleton.class);
            bind(HttpServlet30Dispatcher.class).in(Singleton.class);
            bind(AuthenticationServletFilter.class).in(Singleton.class);

            filter(Endpoints.GRAPHQL).through(AuthenticationServletFilter.class);
            serve(Endpoints.GRAPHQL).with(GraphQLServlet.class);

            Map<String, String> initParam = new HashMap<>();
            initParam.put("jakarta.ws.rs.core.Application", RestApplication.class.getName());
            initParam.put("resteasy.servlet.mapping.prefix", Endpoints.REST);
            serve(Endpoints.REST + "/*").with(HttpServlet30Dispatcher.class, initParam);
          }
        });
  }

  @Provides
  @Singleton
  public TasksConfig provideConfig() throws Exception {
    final TasksConfig config = new TasksConfig();
    config.loadConfig();
    return config;
  }

  @Provides
  @Singleton
  public HikariDataSource provideDataSource(TasksConfig config) {
    String jdbcPostgresUrl = String.format("jdbc:postgresql://%s:%s/%s",
        config.getDatabaseHost(),
        config.getDatabasePort(),
        config.getDatabaseName());

    int maximumPoolSize = config.getHikariMaxPoolSize();
    int minimumIdleConnections = config.getHikariMinIdleConnections();
    int idleTimeout = config.getHikariIdleTimeout();
    int leakDetectionThreshold = config.getHikariLeakDetectionThreshold();
    int maxLifetime = config.getHikariMaxLifetime();

    logger.info("Hikari: maximum pool size: {}", maximumPoolSize);
    logger.info("Hikari: minimum idle connections: {}", minimumIdleConnections);
    logger.info("Hikari: idle timeout: {}", idleTimeout);
    logger.info("Hikari: leak detection threshold: {}", leakDetectionThreshold);
    logger.info("Hikari: max lifetime: {}", maxLifetime);

    Properties dataSourceProperties = new Properties();
    dataSourceProperties.setProperty("sslmode", "disable");
    dataSourceProperties.setProperty("ApplicationName", "tasks");

    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(jdbcPostgresUrl);
    dataSource.setPoolName("tasks-db-pool");
    dataSource.setUsername(config.getDatabaseUsername());
    dataSource.setPassword(config.getDatabasePassword());
    dataSource.setMaximumPoolSize(maximumPoolSize);
    dataSource.setMinimumIdle(minimumIdleConnections);
    dataSource.setIdleTimeout(idleTimeout);
    dataSource.setLeakDetectionThreshold(leakDetectionThreshold);
    dataSource.setMaxLifetime(maxLifetime);
    dataSource.setDataSourceProperties(dataSourceProperties);

    return dataSource;
  }

  @Provides
  @Singleton
  public DatabaseConfig provideEbeanDatabaseConfig(HikariDataSource dataSource) {
    List<Class<?>> entityList = new ArrayList<>();
    entityList.add(Task.class);

    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setName("carbonio-tasks-postgres");
    databaseConfig.setDataSource(dataSource);
    databaseConfig.setDefaultServer(true);
    databaseConfig.addAll(entityList);

    return databaseConfig;
  }

  @Provides
  @Singleton
  public Database provideEbeanDatabase(DatabaseConfig databaseConfig) {
    try {
      Database ebeanDatabase = DatabaseFactory.createWithContextClassLoader(
          databaseConfig,
          TasksModule.class.getClassLoader());

      logger.info("Database connection created successfully");
      return ebeanDatabase;
    } catch (Exception exception) {
      String error = String.format(
          "%s: e.g. %s, %s or %s",
          "Unable to create the database connection! Something went wrong",
          "database is not reachable",
          "the database does not exist",
          "the database credentials are wrong");

      throw new RuntimeException(error, exception);
    }
  }

  @Provides
  @Singleton
  public Flyway provideFlyway(HikariDataSource dataSource) {
    return Flyway.configure()
        .dataSource(dataSource)
        .configuration(
            Map.of("flyway.postgresql.transactional.lock", "false")) // use only one connection
        .baselineOnMigrate(true) // if schema is not empty create baseline, if it is ignore
        .baselineVersion("0")
        .load();
  }

  @Provides
  @Singleton
  public UserManagementClient provideUserManagementClient(TasksConfig config) {
    final String carbonioUserManagementUrl = String.format(
        "%s://%s:%s",
        Constants.Config.UserManagement.DEFAULT_PROTOCOL,
        config.getUserManagementHost(),
        config.getUserManagementPort());

    return UserManagementClient.atURL(carbonioUserManagementUrl);
  }
}