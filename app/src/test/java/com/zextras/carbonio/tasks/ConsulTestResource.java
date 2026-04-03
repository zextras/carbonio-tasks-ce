// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

import com.zextras.carbonio.quarkus.extensions.bootstrap.CarbonioServiceConfig;
import com.zextras.carbonio.quarkus.extensions.bootstrap.ConsulTestHelper;
import com.zextras.carbonio.quarkus.extensions.bootstrap.db.CarbonioDatabaseServiceConfig;

import com.zextras.carbonio.tasks.config.TasksServiceConfig;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.lifecycle.Startables;

/**
 * Starts Consul and PostgreSQL Testcontainers for {@code @QuarkusTest} integration tests.
 *
 * <p>Consul provides the service-discover endpoint consumed by the bootstrap extension. Its KV
 * store is pre-populated with the database credentials needed by the database extension. PostgreSQL
 * provides the actual database that Flyway migrates and Hibernate/Panache connects to.
 */
public class ConsulTestResource implements QuarkusTestResourceLifecycleManager {

  private static final String DB_NAME = "carbonio-tasks-db";
  private static final String DB_USER = "test";
  private static final String DB_PASSWORD = "test";

  private ConsulContainer consul;
  private PostgreSQLContainer<?> postgres;

  @Override
  public Map<String, String> start() {
    consul = new ConsulContainer("hashicorp/consul:1.15");
    postgres =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName(DB_NAME)
            .withUsername(DB_USER)
            .withPassword(DB_PASSWORD);

    Startables.deepStart(consul, postgres).join();

    String consulHost = consul.getHost();
    int consulPort = consul.getFirstMappedPort();

    ConsulTestHelper helper = new ConsulTestHelper(consulHost, consulPort);
    String svc = "carbonio-tasks";
    helper.putValue(
        svc + "/" + CarbonioDatabaseServiceConfig.ApplicationConfig.DB_NAME, DB_NAME);
    helper.putValue(
        svc + "/" + CarbonioDatabaseServiceConfig.ApplicationConfig.DB_USERNAME, DB_USER);
    helper.putValue(
        svc + "/" + CarbonioDatabaseServiceConfig.ApplicationConfig.DB_PASSWORD, DB_PASSWORD);

    String postgresHost = postgres.getHost();
    int postgresPort = postgres.getFirstMappedPort();
    String jdbcUrl = String.format(
        "jdbc:postgresql://%s:%d/%s?sslmode=disable",
        postgresHost, postgresPort, DB_NAME);

    return Map.of(
        CarbonioServiceConfig.NETWORKING_CONFIG_PREFIX
            + CarbonioServiceConfig.NetworkingConfig.SERVICE_DISCOVER_HOST,
        consulHost,
        CarbonioServiceConfig.NETWORKING_CONFIG_PREFIX
            + CarbonioServiceConfig.NetworkingConfig.SERVICE_DISCOVER_PORT,
        String.valueOf(consulPort),
        CarbonioServiceConfig.NETWORKING_CONFIG_PREFIX
            + TasksServiceConfig.NetworkingConfig.POSTGRES_HOST,
        postgresHost,
        CarbonioServiceConfig.NETWORKING_CONFIG_PREFIX
            + TasksServiceConfig.NetworkingConfig.POSTGRES_PORT,
        String.valueOf(postgresPort),
        // Explicit Quarkus datasource override to bypass bootstrap factory timing issues
        "quarkus.datasource.jdbc.url",
        jdbcUrl,
        "quarkus.datasource.username",
        DB_USER,
        "quarkus.datasource.password",
        DB_PASSWORD);
  }

  @Override
  public void stop() {
    if (postgres != null) {
      postgres.stop();
    }
    if (consul != null) {
      consul.stop();
    }
  }
}
