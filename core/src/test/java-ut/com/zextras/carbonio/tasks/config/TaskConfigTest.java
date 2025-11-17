// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.config;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.tasks.Constants;
import com.zextras.carbonio.tasks.Constants.Config.ServiceDiscover;
import com.zextras.carbonio.tasks.dal.dao.Task;
import io.ebean.config.DatabaseConfig;
import java.util.Properties;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;
import org.testcontainers.shaded.com.trilead.ssh2.crypto.Base64;

class TaskConfigTest {

  private static ClientAndServer clientAndServer;
  private static MockServerClient serviceDiscoverMock;

  @BeforeAll
  static void init() {
    clientAndServer = ClientAndServer.startClientAndServer(8500);
    serviceDiscoverMock = new MockServerClient("localhost", 8500);
  }

  @AfterAll
  static void cleanUpAll() {
    clientAndServer.stop();
  }

  @BeforeEach
  void setUp() {
    System.clearProperty(ServiceDiscover.HOST_PROPERTY);
    System.clearProperty(ServiceDiscover.PORT_PROPERTY);
    serviceDiscoverMock.reset();
  }

  @Test
  void havingAnAvailableServiceDiscoverTheTasksConfigShouldReturnADataSource() {
    // Given
    createServiceDiscoverMock();

    // When
    Injector injector = Guice.createInjector(new TasksModule());
    HikariDataSource dataSource = injector.getInstance(HikariDataSource.class);

    // Then
    Assertions.assertThat(dataSource.getJdbcUrl())
        .isEqualTo("jdbc:postgresql://127.78.0.16:20000/fake-db-name");
    Assertions.assertThat(dataSource.getUsername()).isEqualTo("fake-db-username");
    Assertions.assertThat(dataSource.getPassword()).isEqualTo("fake-db-password");

    Properties dataSourceProperties = dataSource.getDataSourceProperties();
    Assertions.assertThat(dataSourceProperties).hasSize(2);
    Assertions.assertThat(dataSourceProperties.getProperty("sslmode")).isEqualTo("disable");
    Assertions.assertThat(dataSourceProperties.getProperty("ApplicationName")).isEqualTo("tasks");

    serviceDiscoverMock.verify(
        HttpRequest.request()
            .withMethod(HttpMethod.GET.toString())
            .withPath("/v1/kv/carbonio-tasks/db-name"),
        VerificationTimes.once());

    serviceDiscoverMock.verify(
        HttpRequest.request()
            .withMethod(HttpMethod.GET.toString())
            .withPath("/v1/kv/carbonio-tasks/db-username"),
        VerificationTimes.once());

    serviceDiscoverMock.verify(
        HttpRequest.request()
            .withMethod(HttpMethod.GET.toString())
            .withPath("/v1/kv/carbonio-tasks/db-password"),
        VerificationTimes.once());
  }

  @Test
  void givenAnUnavailableServiceDiscoverTheTasksConfigShouldReturnADataSource() {
    // Given

    // When
    Injector injector = Guice.createInjector(new TasksModule());
    HikariDataSource dataSource = injector.getInstance(HikariDataSource.class);

    // Then
    Assertions.assertThat(dataSource.getJdbcUrl())
        .isEqualTo("jdbc:postgresql://127.78.0.16:20000/carbonio-tasks-db");
    Assertions.assertThat(dataSource.getUsername()).isEqualTo("carbonio-tasks-db");
    Assertions.assertThat(dataSource.getPassword()).isEmpty();

    Properties dataSourceProperties = dataSource.getDataSourceProperties();
    Assertions.assertThat(dataSourceProperties).hasSize(2);
    Assertions.assertThat(dataSourceProperties.getProperty("sslmode")).isEqualTo("disable");
    Assertions.assertThat(dataSourceProperties.getProperty("ApplicationName")).isEqualTo("tasks");
  }

  @Test
  void givenDatabaseUrlAndPortSystemPropertiesTheTasksConfigShouldReturnADataSource() {
    // Given
    System.setProperty(Constants.Config.Database.HOST_PROPERTY, "different-host");
    System.setProperty(Constants.Config.Database.PORT_PROPERTY, "888");

    // When
    Injector injector = Guice.createInjector(new TasksModule());
    HikariDataSource dataSource = injector.getInstance(HikariDataSource.class);

    // Then
    Assertions.assertThat(dataSource.getJdbcUrl())
        .isEqualTo("jdbc:postgresql://different-host:888/carbonio-tasks-db");
  }

  @Test
  void givenServiceDiscoverHostAndPortSystemPropertyTheTasksConfigShouldReturnThem() {
    // Given
    System.setProperty(ServiceDiscover.HOST_PROPERTY, "other-host");
    System.setProperty(ServiceDiscover.PORT_PROPERTY, "10000");

    // When
    Injector injector = Guice.createInjector(new TasksModule());
    TasksConfig tasksConfig = injector.getInstance(TasksConfig.class);

    // Then
    Assertions.assertThat(tasksConfig.getServiceDiscoverEndpoint())
        .isEqualTo("http://other-host:10000");
  }

  @Test
  void givenServiceDiscoverHostAndPortSystemPropertyEmptyTheTasksConfigShouldReturnDefaultValues() {

    // When
    Injector injector = Guice.createInjector(new TasksModule());
    TasksConfig tasksConfig = injector.getInstance(TasksConfig.class);

    // Then
    Assertions.assertThat(tasksConfig.getServiceDiscoverEndpoint())
        .isEqualTo("http://localhost:8500");
  }

  @Test
  void havingAnAvailableServiceDiscoverTheTasksConfigShouldReturnADatabaseName() {
    // Given
    createServiceDiscoverMock();
    // When
    String databaseName = TasksConfig.getConfig().getDatabaseName();

    // Then
    Assertions.assertThat(databaseName).isEqualTo("fake-db-name");

    serviceDiscoverMock.verify(
        HttpRequest.request()
            .withMethod(HttpMethod.GET.toString())
            .withPath("/v1/kv/carbonio-tasks/db-name"),
        VerificationTimes.once());

    serviceDiscoverMock.verify(
        HttpRequest.request()
            .withMethod(HttpMethod.GET.toString())
            .withPath("/v1/kv/carbonio-tasks/db-username"),
        VerificationTimes.never());

    serviceDiscoverMock.verify(
        HttpRequest.request()
            .withMethod(HttpMethod.GET.toString())
            .withPath("/v1/kv/carbonio-tasks/db-password"),
        VerificationTimes.never());
  }

  @Test
  void withoutAnAvailableServiceDiscoverTheTasksConfigShouldReturnADatabaseName() {
    // Given & When
    String databaseName = TasksConfig.getConfig().getDatabaseName();

    // Then
    Assertions.assertThat(databaseName).isEqualTo("carbonio-tasks-db");
  }

  @Test
  void havingAnAvailableServiceDiscoverTheTasksConfigShouldReturnAnEbeanDatabaseConfig() {
    // Given
    createServiceDiscoverMock();

    // When
    Injector injector = Guice.createInjector(new TasksModule());
    DatabaseConfig databaseConfig = injector.getInstance(DatabaseConfig.class);

    // Then
    Assertions.assertThat(databaseConfig.getName()).isEqualTo("carbonio-tasks-postgres");
    Assertions.assertThat(databaseConfig.isDefaultServer()).isTrue();

    Set<Class<?>> entityClasses = databaseConfig.classes();
    Assertions.assertThat(entityClasses).hasSize(1).contains(Task.class);

    Assertions.assertThat(databaseConfig.getDataSource()).isInstanceOf(HikariDataSource.class);
    HikariDataSource dataSource = (HikariDataSource) databaseConfig.getDataSource();

    Assertions.assertThat(dataSource.getJdbcUrl())
        .isEqualTo("jdbc:postgresql://127.78.0.16:20000/fake-db-name");
    Assertions.assertThat(dataSource.getUsername()).isEqualTo("fake-db-username");
    Assertions.assertThat(dataSource.getPassword()).isEqualTo("fake-db-password");

    Properties dataSourceProperties = dataSource.getDataSourceProperties();
    Assertions.assertThat(dataSourceProperties).hasSize(2);
    Assertions.assertThat(dataSourceProperties.getProperty("sslmode")).isEqualTo("disable");
    Assertions.assertThat(dataSourceProperties.getProperty("ApplicationName")).isEqualTo("tasks");

    serviceDiscoverMock.verify(
        HttpRequest.request()
            .withMethod(HttpMethod.GET.toString())
            .withPath("/v1/kv/carbonio-tasks/db-name"),
        VerificationTimes.once());

    serviceDiscoverMock.verify(
        HttpRequest.request()
            .withMethod(HttpMethod.GET.toString())
            .withPath("/v1/kv/carbonio-tasks/db-username"),
        VerificationTimes.once());

    serviceDiscoverMock.verify(
        HttpRequest.request()
            .withMethod(HttpMethod.GET.toString())
            .withPath("/v1/kv/carbonio-tasks/db-password"),
        VerificationTimes.once());
  }

  @Test
  void withoutAnAvailableServiceDiscoverTheTasksConfigShouldReturnAnEbeanDatabaseConfig() {
    // Given

    // When
    Injector injector = Guice.createInjector(new TasksModule());
    DatabaseConfig databaseConfig = injector.getInstance(DatabaseConfig.class);

    // Then
    Assertions.assertThat(databaseConfig.getName()).isEqualTo("carbonio-tasks-postgres");
    Assertions.assertThat(databaseConfig.isDefaultServer()).isTrue();

    Set<Class<?>> entityClasses = databaseConfig.classes();
    Assertions.assertThat(entityClasses).hasSize(1).contains(Task.class);

    Assertions.assertThat(databaseConfig.getDataSource()).isInstanceOf(HikariDataSource.class);
    HikariDataSource dataSource = (HikariDataSource) databaseConfig.getDataSource();

    Assertions.assertThat(dataSource.getJdbcUrl())
        .isEqualTo("jdbc:postgresql://127.78.0.16:20000/carbonio-tasks-db");
    Assertions.assertThat(dataSource.getUsername()).isEqualTo("carbonio-tasks-db");
    Assertions.assertThat(dataSource.getPassword()).isEmpty();

    Properties dataSourceProperties = dataSource.getDataSourceProperties();
    Assertions.assertThat(dataSourceProperties).hasSize(2);
    Assertions.assertThat(dataSourceProperties.getProperty("sslmode")).isEqualTo("disable");
    Assertions.assertThat(dataSourceProperties.getProperty("ApplicationName")).isEqualTo("tasks");
  }

  private void createServiceDiscoverMock() {
    String encodedDbName = new String(Base64.encode("fake-db-name".getBytes()));
    String encodedDbUsername = new String(Base64.encode("fake-db-username".getBytes()));
    String encodedDbPassword = new String(Base64.encode("fake-db-password".getBytes()));
    String bodyPayloadFormat = "[{\"Key\":\"%s\",\"Value\":\"%s\"}]";

    serviceDiscoverMock
        .when(
            HttpRequest.request()
                .withMethod(HttpMethod.GET.toString())
                .withPath("/v1/kv/carbonio-tasks/db-name")
                .withHeader("X-Consul-Token", ""))
        .respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withBody(
                    String.format(bodyPayloadFormat, "carbonio-tasks/db-name", encodedDbName)));

    serviceDiscoverMock
        .when(
            HttpRequest.request()
                .withMethod(HttpMethod.GET.toString())
                .withPath("/v1/kv/carbonio-tasks/db-username")
                .withHeader("X-Consul-Token", ""))
        .respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withBody(
                    String.format(
                        bodyPayloadFormat, "carbonio-tasks/db-username", encodedDbUsername)));

    serviceDiscoverMock
        .when(
            HttpRequest.request()
                .withMethod(HttpMethod.GET.toString())
                .withPath("/v1/kv/carbonio-tasks/db-password")
                .withHeader("X-Consul-Token", ""))
        .respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withBody(
                    String.format(
                        bodyPayloadFormat, "carbonio-tasks/db-password", encodedDbPassword)));
  }
}
