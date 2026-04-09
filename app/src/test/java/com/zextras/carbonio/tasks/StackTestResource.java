// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

import com.zextras.carbonio.quarkus.extensions.bootstrap.ConsulTestHelper;
import com.zextras.carbonio.quarkus.extensions.bootstrap.db.CarbonioDatabaseServiceConfig;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;

/**
 * Full integration test stack for carbonio-tasks-ce.
 *
 * <p>Starts: openldap, mariadb, postfix, mailbox, user-management (all on shared Docker network)
 * plus consul (on the same network, accessible from host via mapped port) and an independent
 * PostgreSQL container.
 *
 * <p>After startup: provisions test user via {@code zmprov}, authenticates via SOAP to get a real
 * {@code ZM_AUTH_TOKEN}, and pre-populates Consul KV with tasks DB credentials.
 *
 * <p>Tests read {@link #AUTH_TOKEN} and {@link #TEST_USER_ID} from static fields.
 */
public class StackTestResource implements QuarkusTestResourceLifecycleManager {

  private static final String DB_NAME = "carbonio-tasks-db";
  private static final String DB_USER = "test";
  private static final String DB_PASSWORD = "test";
  private static final String TEST_USER_EMAIL = "test-user@carbonio.localhost";
  private static final String TEST_PASSWORD = "test-password";

  /** Real {@code ZM_AUTH_TOKEN} for the provisioned test user. Set during {@code start()}. */
  public static volatile String AUTH_TOKEN;

  /** {@code zimbraId} of the provisioned test user. Set during {@code start()}. */
  public static volatile String TEST_USER_ID;

  /**
   * JDBC URL for the tasks PostgreSQL container. Used by {@code @QuarkusIntegrationTest} classes
   * for direct DB cleanup between tests (no {@code @Inject} available in integration test mode).
   */
  public static volatile String POSTGRES_JDBC_URL;

  private Network network;
  private GenericContainer<?> openldap;
  private GenericContainer<?> mariadb;
  private GenericContainer<?> postfix;
  private GenericContainer<?> mailbox;
  private GenericContainer<?> userManagement;
  private ConsulContainer consul;
  private PostgreSQLContainer<?> postgres;

  @Override
  public Map<String, String> start() {
    network = Network.newNetwork();

    openldap =
        new GenericContainer<>("registry.dev.zextras.com/dev/carbonio-openldap:latest")
            .withNetwork(network)
            .withNetworkAliases("carbonio-openldap")
            .withExposedPorts(1389)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)));

    mariadb =
        new GenericContainer<>("registry.dev.zextras.com/dev/carbonio-mariadb:latest")
            .withNetwork(network)
            .withNetworkAliases("carbonio-mariadb")
            .withEnv("MARIADB_ROOT_PASSWORD", "password")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

    postfix =
        new GenericContainer<>("registry.dev.zextras.com/dev/carbonio-mta:latest")
            .withNetwork(network)
            .withNetworkAliases("carbonio-postfix")
            .withEnv("LDAP_HOST", "carbonio-openldap")
            .withEnv("LDAP_PORT", "1389")
            .withEnv("LDAP_ROOT_PASSWORD", "qh6hWZvc")
            .withEnv("LDAP_ADMIN_PASSWORD", "password")
            .withExposedPorts(25)
            .dependsOn(openldap)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)));

    mailbox =
        new GenericContainer<>("registry.dev.zextras.com/dev/carbonio-mailbox:latest")
            .withNetwork(network)
            .withNetworkAliases("carbonio-mailbox")
            .withCreateContainerCmdModifier(cmd -> cmd.withHostName("docker.carbonio.localhost"))
            .withEnv("LDAP_URL", "ldap://carbonio-openldap:1389")
            .withEnv("LDAP_ROOT_PASSWORD", "qh6hWZvc")
            .withEnv("LDAP_ADMIN_PASSWORD", "password")
            .withEnv("MARIADB_ROOT_PASSWORD", "password")
            .withEnv("MARIADB_URL", "carbonio-mariadb")
            .withEnv("MARIADB_PORT", "3306")
            .withExposedPorts(8080)
            .dependsOn(openldap, postfix, mariadb)
            .waitingFor(
                Wait.forHttp("/service/health/ready")
                    .forPort(8080)
                    .withStartupTimeout(Duration.ofMinutes(10)));

    consul =
        new ConsulContainer("hashicorp/consul:1.22.3")
            .withNetwork(network)
            .withNetworkAliases("consul");

    userManagement =
        new GenericContainer<>(
                "registry.dev.zextras.com/dev/carbonio-user-management:devel")
            .withNetwork(network)
            .withNetworkAliases("carbonio-user-management")
            .withExposedPorts(10000)  // gRPC and HTTP share port 10000 (use-separate-server=false)
            .withEnv("NETWORKING_CONFIG_CARBONIO_SERVICE_HOST", "0.0.0.0")
            .withEnv("NETWORKING_CONFIG_CARBONIO_SERVICE_PORT", "10000")
            .withEnv("NETWORKING_CONFIG_CARBONIO_SERVICE_DISCOVER_HOST", "consul")
            .withEnv("NETWORKING_CONFIG_CARBONIO_SERVICE_DISCOVER_PORT", "8500")
            .withEnv("NETWORKING_CONFIG_CARBONIO_MAILBOX_HOST", "carbonio-mailbox")
            .withEnv("NETWORKING_CONFIG_CARBONIO_MAILBOX_PORT", "8080")
            .dependsOn(mailbox, consul)
            .waitingFor(
                Wait.forHttp("/q/health/live")
                    .forPort(10000)
                    .withStartupTimeout(Duration.ofMinutes(5)));

    postgres =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName(DB_NAME)
            .withUsername(DB_USER)
            .withPassword(DB_PASSWORD);

    // deepStart resolves the dependsOn graph; postgres starts in parallel with the
    // mailbox dependency chain.
    Startables.deepStart(userManagement, postgres).join();

    provisionTestAccount();
    AUTH_TOKEN = soapAuthenticate("http://localhost:" + mailbox.getMappedPort(8080));
    TEST_USER_ID = resolveTestUserId();

    // Pre-populate tasks-ce DB credentials in Consul KV so the database extension finds them.
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

    POSTGRES_JDBC_URL =
        String.format(
            "jdbc:postgresql://%s:%d/%s?sslmode=disable",
            postgres.getHost(), postgres.getFirstMappedPort(), DB_NAME);

    return Map.ofEntries(
        // Service identity
        Map.entry("networking-config.carbonio.service.host", "localhost"),
        // Consul (tasks-ce service discovery)
        Map.entry("networking-config.carbonio.service-discover.host", consulHost),
        Map.entry(
            "networking-config.carbonio.service-discover.port", String.valueOf(consulPort)),
        // PostgreSQL (tasks database)
        Map.entry(
            "networking-config.carbonio.postgresql.host", postgres.getHost()),
        Map.entry(
            "networking-config.carbonio.postgresql.port",
            String.valueOf(postgres.getFirstMappedPort())),
        Map.entry("quarkus.datasource.jdbc.url", POSTGRES_JDBC_URL),
        Map.entry("quarkus.datasource.username", DB_USER),
        Map.entry("quarkus.datasource.password", DB_PASSWORD),
        // User Management gRPC client
        Map.entry("networking-config.carbonio.user-management.host", "localhost"),
        Map.entry(
            "networking-config.carbonio.user-management.port",
            String.valueOf(userManagement.getMappedPort(10000))));
  }

  @Override
  public void stop() {
    stopQuietly(userManagement);
    stopQuietly(mailbox);
    stopQuietly(postfix);
    stopQuietly(mariadb);
    stopQuietly(openldap);
    if (consul != null) {
      try { consul.stop(); } catch (Exception ignored) {}
    }
    if (postgres != null) {
      try { postgres.stop(); } catch (Exception ignored) {}
    }
    if (network != null) {
      network.close();
    }
  }

  private void stopQuietly(GenericContainer<?> container) {
    if (container != null) {
      try {
        container.stop();
      } catch (Exception ignored) {
      }
    }
  }

  private void provisionTestAccount() {
    try {
      mailbox.execInContainer(
          "sh",
          "-c",
          "for i in $(seq 1 30); do "
              + "  echo 'gd carbonio.localhost' | zmprov 2>&1 | grep -qv ERROR && break; "
              + "  sleep 2; "
              + "done && "
              + "zmprov <<'EOF'\n"
              + "cd carbonio.localhost\n"
              + "mcf zimbraSmtpHostname carbonio-postfix\n"
              + "mcf zimbraDefaultDomainName carbonio.localhost\n"
              + "ca "
              + TEST_USER_EMAIL
              + " "
              + TEST_PASSWORD
              + "\n"
              + "EOF");
    } catch (Exception e) {
      throw new RuntimeException("Failed to provision test account", e);
    }
  }

  private String resolveTestUserId() {
    try {
      var result =
          mailbox.execInContainer(
              "sh",
              "-c",
              "zmprov ga " + TEST_USER_EMAIL + " zimbraId | grep zimbraId: | awk '{print $2}'");
      return result.getStdout().trim();
    } catch (Exception e) {
      throw new RuntimeException("Failed to resolve test user ID", e);
    }
  }

  private String soapAuthenticate(String mailboxBaseUrl) {
    String soapBody =
        "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">"
            + "<soap:Body>"
            + "<AuthRequest xmlns=\"urn:zimbraAccount\">"
            + "<account by=\"name\">"
            + TEST_USER_EMAIL
            + "</account>"
            + "<password>"
            + TEST_PASSWORD
            + "</password>"
            + "</AuthRequest>"
            + "</soap:Body>"
            + "</soap:Envelope>";
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(mailboxBaseUrl + "/service/soap/AuthRequest"))
              .header("Content-Type", "application/soap+xml; charset=utf-8")
              .POST(HttpRequest.BodyPublishers.ofString(soapBody))
              .build();
      HttpResponse<String> response =
          HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
      Matcher matcher =
          Pattern.compile("<authToken[^>]*>([^<]+)</authToken>").matcher(response.body());
      if (!matcher.find()) {
        throw new RuntimeException("No authToken in SOAP response: " + response.body());
      }
      return matcher.group(1);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("SOAP auth interrupted", e);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("SOAP auth failed", e);
    }
  }
}
