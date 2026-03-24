// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.util.Modules;
import com.zextras.carbonio.tasks.Constants.Config.Database;
import com.zextras.carbonio.tasks.config.TasksModule;
import com.zextras.carbonio.tasks.dal.DatabaseManager;
import com.zextras.carbonio.user_management.sdk.grpc.GetUserMyselfRequest;
import com.zextras.carbonio.user_management.sdk.grpc.UserInfoProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserManagementServiceGrpc;
import com.zextras.carbonio.user_management.sdk.grpc.UserManagementServiceGrpc.UserManagementServiceBlockingStub;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfResponse;
import com.zextras.carbonio.user_management.sdk.grpc.UserTypeProto;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import jakarta.servlet.DispatcherType;
import java.io.IOException;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.LocalConnector;
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
  private static final String UM_INPROCESS_SERVER_NAME = "um-test";

  private Injector injector;
  private PostgreSQLContainer<?> postgreSQLContainer;
  private ClientAndServer clientAndServer;
  private MockServerClient serviceDiscoverMock;
  private Server umGrpcServer;
  private MockUserManagementService umMockService;
  private ManagedChannel umChannel;
  private org.eclipse.jetty.server.Server jettyServer;
  private LocalConnector httpLocalConnector;
  private boolean isJettyServerEnabled;
  private boolean isUserManagementEnabled;

  public Simulator() {
    isJettyServerEnabled = false;
    isUserManagementEnabled = false;
  }

  private Simulator createInjector() {
    // Always override the ManagedChannel to use InProcessChannel for test isolation.
    // When isUserManagementEnabled=true, the InProcessServer is running so the channel
    // connects successfully. When false, the channel is shut down after injector creation
    // so the health check reports SHUTDOWN (unhealthy).
    umChannel = InProcessChannelBuilder.forName(UM_INPROCESS_SERVER_NAME)
        .directExecutor()
        .build();
    injector = Guice.createInjector(
        Modules.override(new TasksModule()).with(new AbstractModule() {
          @Provides
          @Singleton
          public ManagedChannel provideUserManagementChannel() {
            return umChannel;
          }

          @Provides
          @Singleton
          public UserManagementServiceBlockingStub provideUserManagementStub(
              ManagedChannel channel) {
            return UserManagementServiceGrpc.newBlockingStub(channel);
          }
        }));
    return this;
  }

  private Simulator startDatabaseContainer() {

    if (postgreSQLContainer == null) {
      postgreSQLContainer = new PostgreSQLContainer<>("postgres:16");
    }

    postgreSQLContainer.start();

    // Set the System.properties for the dynamic database url and port
    System.setProperty(Database.HOST_PROPERTY, postgreSQLContainer.getHost());
    System.setProperty(Database.PORT_PROPERTY, String.valueOf(postgreSQLContainer.getFirstMappedPort()));

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

      dbName = Database.DEFAULT_NAME;
      dbUsername = Database.DEFAULT_USERNAME;
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
    umMockService = new MockUserManagementService();
    try {
      umGrpcServer = InProcessServerBuilder.forName(UM_INPROCESS_SERVER_NAME)
          .directExecutor()
          .addService(umMockService)
          .build()
          .start();
    } catch (IOException e) {
      throw new RuntimeException("Failed to start User Management gRPC in-process server", e);
    }
    isUserManagementEnabled = true;
    return this;
  }

  private void registerUser(String token, String userId) {
    UserInfoProto userInfo = UserInfoProto.newBuilder()
        .setUserId(userId)
        .setEmail("fake-email@example.com")
        .setFullName("Fake User")
        .setDomain("example.com")
        .setStatus("active")
        .setType(UserTypeProto.INTERNAL)
        .build();

    UserMyselfProto userMyself = UserMyselfProto.newBuilder()
        .setInfo(userInfo)
        .setLocale("en")
        .addFeatures("carbonioFeatureTasksEnabled")
        .build();

    umMockService.registerToken(token, userMyself);
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
    if (umGrpcServer != null) {
      umGrpcServer.shutdownNow();
      umGrpcServer = null;
    }
    if (umChannel != null) {
      umChannel.shutdownNow();
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
      jettyServer = new org.eclipse.jetty.server.Server();
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
      users.forEach(simulator::registerUser);
      return this;
    }

    public Simulator withServer() {
      simulator.enableJettyServer();
      return simulator;
    }

    public SimulatorBuilder withMinimalServices() {
      // Since db and service discover are required we consider it minimal config to even start tasks.
      this.withDatabase().withServiceDiscover();
      simulator.enableJettyServer();
      return this;
    }

    public Simulator build() {
      simulator.createInjector();
      // If UM was not started, shut down the channel so the health check sees SHUTDOWN
      // (not IDLE, which would be reported as healthy).
      if (!simulator.isUserManagementEnabled && simulator.umChannel != null) {
        simulator.umChannel.shutdownNow();
      }
      boolean postgreIsRunning = simulator.postgreSQLContainer != null && simulator.postgreSQLContainer.isRunning();
      boolean serviceDiscoverIsRunning = simulator.serviceDiscoverMock != null && simulator.serviceDiscoverMock.hasStarted();
      if (postgreIsRunning && serviceDiscoverIsRunning) {
        simulator.initializeDatabase();
      }
      if (postgreIsRunning && !serviceDiscoverIsRunning) {
        logger.warn("Database not initialized since service discover is not running (add withServiceDiscover to your simulator builder to initialize database)");
      }
      if (!postgreIsRunning && serviceDiscoverIsRunning) {
        logger.warn("Database not initialized since database container is not running (add withDatabase to your simulator builder to initialize database)");
      }
      return simulator;
    }
  }

  /**
   * In-process gRPC implementation of UserManagementService for integration tests.
   */
  public static class MockUserManagementService
      extends UserManagementServiceGrpc.UserManagementServiceImplBase {

    private final Map<String, UserMyselfProto> tokenToUserMyself = new HashMap<>();

    public void registerToken(String token, UserMyselfProto userMyself) {
      tokenToUserMyself.put(token, userMyself);
    }

    @Override
    public void getUserMyself(
        GetUserMyselfRequest request, StreamObserver<UserMyselfResponse> responseObserver) {
      String token = request.getToken();
      UserMyselfProto userMyself = tokenToUserMyself.get(token);
      if (userMyself == null) {
        responseObserver.onError(
            Status.UNAUTHENTICATED.withDescription("Invalid token").asRuntimeException());
        return;
      }
      responseObserver.onNext(UserMyselfResponse.newBuilder().setUser(userMyself).build());
      responseObserver.onCompleted();
    }
  }
}
