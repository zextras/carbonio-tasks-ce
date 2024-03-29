// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.zextras.carbonio.tasks.Constants.Config.Database;
import com.zextras.carbonio.tasks.Constants.Config.Properties;
import com.zextras.carbonio.tasks.Constants.Config.UserService;
import com.zextras.carbonio.tasks.config.TasksModule;
import com.zextras.carbonio.tasks.dal.DatabaseManager;
import jakarta.servlet.DispatcherType;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Map;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.trilead.ssh2.crypto.Base64;

@Testcontainers
public class Simulator implements AutoCloseable {

  public static final String DATABASE_PASSWORD = "test-password";
  private static final Logger logger = LoggerFactory.getLogger(Simulator.class);
  private Injector injector;
  private PostgreSQLContainer<?> postgreSQLContainer;
  private ClientAndServer clientAndServer;
  private MockServerClient serviceDiscoverMock;
  private MockServerClient userManagementMock;
  private Server jettyServer;
  private LocalConnector httpLocalConnector;
  private boolean isJettyServerEnabled;

  public Simulator() {
    isJettyServerEnabled = false;
  }

  private Simulator createInjector() {
    injector = Guice.createInjector(new TasksModule());
    return this;
  }

  private Simulator startDatabaseContainer() {

    if (postgreSQLContainer == null) {
      postgreSQLContainer = new PostgreSQLContainer<>("postgres:12.14");
    }

    postgreSQLContainer.start();

    // Set the System.properties for the dynamic database url and port
    System.setProperty(Properties.DATABASE_URL, postgreSQLContainer.getHost());
    System.setProperty(
        Properties.DATABASE_PORT, String.valueOf(postgreSQLContainer.getFirstMappedPort()));

    return this;
  }

  private Simulator initializeDatabase() {
    DatabaseManager databaseManager = injector.getInstance(DatabaseManager.class);
    databaseManager.initialize();
    return this;
  }

  private Simulator startServiceDiscover() {

    startMockServer();
    serviceDiscoverMock = new MockServerClient("localhost", 8500);

    String dbName;
    String dbUsername;
    String dbPassword;

    if (postgreSQLContainer != null && postgreSQLContainer.isRunning()) {
      dbName = postgreSQLContainer.getDatabaseName();
      dbUsername = postgreSQLContainer.getUsername();
      dbPassword = postgreSQLContainer.getPassword();
    } else {
      logger.warn(
          "The ServiceDiscover will be mocked without a database container. The database "
              + "credentials are the default one specified in the Constants class");

      dbName = Database.NAME;
      dbUsername = Database.USERNAME;
      dbPassword = DATABASE_PASSWORD;
    }

    String encodedDbName = new String(Base64.encode(dbName.getBytes()));
    String encodedDbUsername = new String(Base64.encode(dbUsername.getBytes()));
    String encodedDbPassword = new String(Base64.encode(dbPassword.getBytes()));
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

    return this;
  }

  public Simulator startUserManagement() {

    startMockServer();
    userManagementMock = new MockServerClient(UserService.URL, UserService.PORT);
    return this;
  }

  private void validateUser(String cookie, String userId) {
    userManagementMock
        .when(
            HttpRequest.request()
                .withMethod(HttpMethod.GET.toString())
                .withPath("/auth/token/" + cookie))
        .respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withBody("{\"userId\":\"" + userId + "\"}"));
  }

  public Simulator enableJettyServer() {
    isJettyServerEnabled = true;
    return this;
  }

  public void stopAll() {
    stopJettyServer();
    stopUserManagement();
    stopServiceDiscover();
    stopDatabase();
  }

  private Simulator stopDatabase() {
    if (postgreSQLContainer != null && postgreSQLContainer.isRunning()) {
      postgreSQLContainer.stop();
    }

    return this;
  }

  private Simulator stopJettyServer() {
    if (jettyServer != null) {
      try {
        jettyServer.stop();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return this;
  }

  private Simulator stopServiceDiscover() {
    if (serviceDiscoverMock != null && serviceDiscoverMock.hasStarted()) {
      serviceDiscoverMock.stop();
    }

    return this;
  }

  private Simulator stopUserManagement() {
    if (userManagementMock != null && userManagementMock.hasStarted()) {
      userManagementMock.stop();
    }

    return this;
  }

  public Simulator start() {
    if (isJettyServerEnabled) {
      startJettyServer();
    }

    return this;
  }

  public Injector getInjector() {
    return injector;
  }

  public MockServerClient getServiceDiscoverMock() {
    return serviceDiscoverMock;
  }

  public MockServerClient getUserManagementMock() {
    return userManagementMock;
  }

  public LocalConnector getHttpLocalConnector() {
    return httpLocalConnector;
  }

  private void startMockServer() {
    if (clientAndServer == null) {
      clientAndServer = ClientAndServer.startClientAndServer(8500, UserService.PORT);
    }
  }

  private void startJettyServer() {
    try {
      jettyServer = new Server();
      httpLocalConnector = new LocalConnector(jettyServer);
      jettyServer.addConnector(httpLocalConnector);

      ServletContextHandler servletContextHandler =
          new ServletContextHandler("/", ServletContextHandler.SESSIONS);

      servletContextHandler.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
      servletContextHandler.addEventListener(
          injector.getInstance(GuiceResteasyBootstrapServletContextListener.class));

      jettyServer.setHandler(servletContextHandler);
      jettyServer.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void resetDatabase() {
    try {
      postgreSQLContainer.createConnection("").createStatement().execute("DELETE FROM task;");
    } catch (SQLException e) {
      logger.error("Unable to delete all the records in the Task database table");
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() {
    stopAll();
  }

  public static class SimulatorBuilder {

    private Simulator simulator;

    public static SimulatorBuilder aSimulator() {
      return new SimulatorBuilder();
    }

    public SimulatorBuilder init() {
      simulator = new Simulator();
      return this;
    }

    public SimulatorBuilder withDatabase() {
      simulator.startDatabaseContainer();
      return this;
    }

    public SimulatorBuilder withServiceDiscover() {
      simulator.startServiceDiscover();
      return this;
    }

    public SimulatorBuilder withUserManagement(Map<String, String> users) {
      simulator.startUserManagement();
      users.forEach((cookie, userId) -> simulator.validateUser(cookie, userId));
      return this;
    }

    public SimulatorBuilder withServer() {
      simulator.enableJettyServer();
      return this;
    }

    public Simulator build() {
      simulator.createInjector();
      boolean postgreIsRunning = simulator.postgreSQLContainer != null && simulator.postgreSQLContainer.isRunning();
      boolean serviceDiscoverIsRunning = simulator.serviceDiscoverMock != null && simulator.serviceDiscoverMock.hasStarted();
      if(postgreIsRunning && serviceDiscoverIsRunning){
        simulator.initializeDatabase();
      }
      if(postgreIsRunning && !serviceDiscoverIsRunning){
        logger.warn("Database not initialized since service discover is not running (add withServiceDiscover to your simulator builder to initialize database)");
      }
      if(!postgreIsRunning && serviceDiscoverIsRunning){
        logger.warn("Database not initialized since database container is not running (add withDatabase to your simulator builder to initialize database)");
      }
      return simulator;
    }
  }
}
