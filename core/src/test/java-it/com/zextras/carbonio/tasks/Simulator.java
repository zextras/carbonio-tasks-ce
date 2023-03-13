// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.zextras.carbonio.tasks.Constants.Config.Database;
import com.zextras.carbonio.tasks.Constants.Config.Properties;
import com.zextras.carbonio.tasks.Constants.Service.API.Endpoints;
import com.zextras.carbonio.tasks.config.TasksModule;
import com.zextras.carbonio.tasks.graphql.GraphQLServlet;
import com.zextras.carbonio.tasks.rest.RestApplication;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.trilead.ssh2.crypto.Base64;
import org.testcontainers.utility.MountableFile;

@Testcontainers
public class Simulator implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(Simulator.class);

  private Injector injector;
  private PostgreSQLContainer<?> postgreSQLContainer;
  private ClientAndServer clientAndServer;
  private MockServerClient serviceDiscoverMock;
  private MockServerClient userManagementMock;
  private Server jettyServer;
  private LocalConnector httpLocalConnector;
  private ServletContextHandler graphQlServletContextHandler;
  private ServletContextHandler restServletContextHandler;

  private Simulator createInjector() {
    injector = Guice.createInjector(new TasksModule());
    return this;
  }

  private Simulator startDatabase() {

    if (postgreSQLContainer == null) {
      postgreSQLContainer = new PostgreSQLContainer<>("postgres:12.14");
      postgreSQLContainer.withCopyFileToContainer(
          MountableFile.forClasspathResource("/sql/postgresql_1.sql"),
          "/docker-entrypoint-initdb.d/init.sql");
    }

    postgreSQLContainer.start();

    // Set the System.properties for the dynamic database url and port
    System.setProperty(Properties.DATABASE_URL, postgreSQLContainer.getHost());
    System.setProperty(
        Properties.DATABASE_PORT, String.valueOf(postgreSQLContainer.getFirstMappedPort()));

    return this;
  }

  private Simulator startServiceDiscover() {

    startMockServer();
    serviceDiscoverMock = new MockServerClient("localhost", 8500);

    String dbName;
    String dbUsername;
    String dbPassword;

    if (postgreSQLContainer.isRunning()) {
      dbName = postgreSQLContainer.getDatabaseName();
      dbUsername = postgreSQLContainer.getUsername();
      dbPassword = postgreSQLContainer.getPassword();
    } else {
      logger.warn(
          "The ServiceDiscover will be mocked without a database container. The database "
              + "credentials are the default one specified in the Constants class");

      dbName = Database.NAME;
      dbUsername = Database.USERNAME;
      dbPassword = "";
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

    if (userManagementMock == null || userManagementMock.hasStopped()) {
      // TODO implement this method when the authentication are done
    }

    return this;
  }

  public Simulator createRestServlet() {
    restServletContextHandler = new ServletContextHandler();
    restServletContextHandler.setContextPath(Endpoints.REST);
    ServletHolder restServletHolder =
        restServletContextHandler.addServlet(HttpServlet30Dispatcher.class, "/*");
    restServletHolder.setInitParameter("javax.ws.rs.Application", RestApplication.class.getName());

    return this;
  }

  public Simulator createGraphQlServlet() {
    graphQlServletContextHandler = new ServletContextHandler();
    graphQlServletContextHandler.setContextPath(Endpoints.GRAPHQL);
    GraphQLServlet graphQLServlet = injector.getInstance(GraphQLServlet.class);
    ServletHolder graphQLServletHolder = new ServletHolder("graphql-servlet", graphQLServlet);
    graphQlServletContextHandler.addServlet(graphQLServletHolder, "/");

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
    if (graphQlServletContextHandler != null || restServletContextHandler != null) {
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
      clientAndServer = ClientAndServer.startClientAndServer(8500);
    }
  }

  private void startJettyServer() {
    try {
      jettyServer = new Server();
      httpLocalConnector = new LocalConnector(jettyServer);
      jettyServer.addConnector(httpLocalConnector);

      ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();

      if (graphQlServletContextHandler != null) {
        contextHandlerCollection.addHandler(graphQlServletContextHandler);
      }

      if (restServletContextHandler != null) {
        contextHandlerCollection.addHandler(restServletContextHandler);
      }

      jettyServer.setHandler(contextHandlerCollection);
      jettyServer.start();
    } catch (Exception e) {
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
      simulator.createInjector();
      return this;
    }

    public SimulatorBuilder withDatabase() {
      simulator.startDatabase();
      return this;
    }

    public SimulatorBuilder withServiceDiscover() {
      simulator.startServiceDiscover();
      return this;
    }

    public SimulatorBuilder withUserManagement() {
      simulator.startUserManagement();
      return this;
    }

    public SimulatorBuilder withGraphQlServlet() {
      simulator.createGraphQlServlet();
      return this;
    }

    public SimulatorBuilder withRestServlet() {
      simulator.createRestServlet();
      return this;
    }

    public Simulator build() {
      return simulator;
    }
  }
}
