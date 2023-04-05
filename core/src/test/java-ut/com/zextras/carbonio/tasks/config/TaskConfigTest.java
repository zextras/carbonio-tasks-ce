// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.config;

import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.tasks.dal.dao.DbInfo;
import com.zextras.carbonio.tasks.dal.dao.Task;
import io.ebean.config.DatabaseConfig;
import java.util.List;
import java.util.Properties;
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
    serviceDiscoverMock.reset();
  }

  @Test
  void havingAnAvailableServiceDiscoverTheTasksConfigShouldReturnADataSource() {
    // Given
    createServiceDiscoverMock();

    // When
    HikariDataSource dataSource = new TasksConfig().getDataSource();

    // Then
    Assertions.assertThat(dataSource.getJdbcUrl())
        .isEqualTo("jdbc:postgresql://127.78.0.16:20000/fake-db-name");
    Assertions.assertThat(dataSource.getUsername()).isEqualTo("fake-db-username");
    Assertions.assertThat(dataSource.getPassword()).isEqualTo("fake-db-password");

    Properties dataSourceProperties = dataSource.getDataSourceProperties();
    Assertions.assertThat(dataSourceProperties).hasSize(1);
    Assertions.assertThat(dataSourceProperties.getProperty("sslmode")).isEqualTo("disable");

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
    HikariDataSource dataSource = new TasksConfig().getDataSource();

    // Then
    Assertions.assertThat(dataSource.getJdbcUrl())
        .isEqualTo("jdbc:postgresql://127.78.0.16:20000/carbonio-tasks-db");
    Assertions.assertThat(dataSource.getUsername()).isEqualTo("carbonio-tasks-db");
    Assertions.assertThat(dataSource.getPassword()).isEmpty();

    Properties dataSourceProperties = dataSource.getDataSourceProperties();
    Assertions.assertThat(dataSourceProperties).hasSize(1);
    Assertions.assertThat(dataSourceProperties.getProperty("sslmode")).isEqualTo("disable");
  }

  @Test
  void givenDatabaseUrlAndPortSystemPropertiesTheTasksConfigShouldReturnADataSource() {
    // Given
    System.setProperty("carbonio.tasks.db.url", "different-url");
    System.setProperty("carbonio.tasks.db.port", "888");

    // When
    HikariDataSource dataSource = new TasksConfig().getDataSource();

    // Then
    Assertions.assertThat(dataSource.getJdbcUrl())
        .isEqualTo("jdbc:postgresql://different-url:888/carbonio-tasks-db");
  }

  @Test
  void havingAnAvailableServiceDiscoverTheTasksConfigShouldReturnADatabaseName() {
    // Given
    createServiceDiscoverMock();
    // When
    String databaseName = new TasksConfig().getDatabaseName();

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
    String databaseName = new TasksConfig().getDatabaseName();

    // Then
    Assertions.assertThat(databaseName).isEqualTo("carbonio-tasks-db");
  }

  @Test
  void havingAnAvailableServiceDiscoverTheTasksConfigShouldReturnAnEbeanDatabaseConfig() {
    // Given
    createServiceDiscoverMock();
    // When
    DatabaseConfig databaseConfig = new TasksConfig().getEbeanDatabaseConfig();

    // Then
    Assertions.assertThat(databaseConfig.getName()).isEqualTo("carbonio-tasks-postgres");
    Assertions.assertThat(databaseConfig.isDefaultServer()).isTrue();

    List<Class<?>> entityClasses = databaseConfig.getClasses();
    Assertions.assertThat(entityClasses).hasSize(2).contains(DbInfo.class).contains(Task.class);

    Assertions.assertThat(databaseConfig.getDataSource()).isInstanceOf(HikariDataSource.class);
    HikariDataSource dataSource = (HikariDataSource) databaseConfig.getDataSource();

    Assertions.assertThat(dataSource.getJdbcUrl())
        .isEqualTo("jdbc:postgresql://127.78.0.16:20000/fake-db-name");
    Assertions.assertThat(dataSource.getUsername()).isEqualTo("fake-db-username");
    Assertions.assertThat(dataSource.getPassword()).isEqualTo("fake-db-password");

    Properties dataSourceProperties = dataSource.getDataSourceProperties();
    Assertions.assertThat(dataSourceProperties).hasSize(1);
    Assertions.assertThat(dataSourceProperties.getProperty("sslmode")).isEqualTo("disable");

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
    DatabaseConfig databaseConfig = new TasksConfig().getEbeanDatabaseConfig();

    // Then
    Assertions.assertThat(databaseConfig.getName()).isEqualTo("carbonio-tasks-postgres");
    Assertions.assertThat(databaseConfig.isDefaultServer()).isTrue();

    List<Class<?>> entityClasses = databaseConfig.getClasses();
    Assertions.assertThat(entityClasses).hasSize(2).contains(DbInfo.class).contains(Task.class);

    Assertions.assertThat(databaseConfig.getDataSource()).isInstanceOf(HikariDataSource.class);
    HikariDataSource dataSource = (HikariDataSource) databaseConfig.getDataSource();

    Assertions.assertThat(dataSource.getJdbcUrl())
        .isEqualTo("jdbc:postgresql://127.78.0.16:20000/carbonio-tasks-db");
    Assertions.assertThat(dataSource.getUsername()).isEqualTo("carbonio-tasks-db");
    Assertions.assertThat(dataSource.getPassword()).isEmpty();

    Properties dataSourceProperties = dataSource.getDataSourceProperties();
    Assertions.assertThat(dataSourceProperties).hasSize(1);
    Assertions.assertThat(dataSourceProperties.getProperty("sslmode")).isEqualTo("disable");
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
