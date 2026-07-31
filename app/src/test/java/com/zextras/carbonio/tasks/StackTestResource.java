// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;

/**
 * Integration test stack for carbonio-tasks-ce.
 *
 * <p><b>Testing philosophy (narrow integration tests):</b>
 *
 * <ul>
 *   <li>Direct dependencies of tasks-ce are run as real Docker containers: {@code
 *       carbonio-user-management} (REST auth validation).
 *   <li>Indirect dependencies (dependencies of our direct deps) are replaced with lightweight mocks
 *       so that our IT suite is isolated from their failures. Specifically, {@code
 *       carbonio-mailbox} — which user-management calls for token validation via its internal REST
 *       API — is stubbed by WireMock. mailbox's own integration with LDAP, MariaDB, and Postfix is
 *       covered by user-management's integration test suite, not ours.
 *   <li>Consul is also stubbed by the same WireMock container (via a second network alias) so no
 *       real Consul container is needed.
 * </ul>
 *
 * <p>Containers are static singletons: they start once per JVM and are reused across all
 * {@code @QuarkusIntegrationTest} classes. {@code stop()} is a no-op; Testcontainers' JVM shutdown
 * hook handles cleanup.
 */
public class StackTestResource implements QuarkusTestResourceLifecycleManager {

  private static final String DB_NAME = "carbonio-tasks-db";
  private static final String DB_USER = "test";
  private static final String DB_PASSWORD = "test";

  private static volatile boolean started = false;
  private static Map<String, String> cachedConfig;

  /**
   * Fixed {@code ZM_AUTH_TOKEN} used by tests and matched by the WireMock stub. Any other token
   * value will receive no stub match → WireMock returns 404 → user-management treats the token as
   * invalid → tasks-ce returns 401.
   */
  public static final String AUTH_TOKEN = "test-auth-token-tasks-ce";

  /** Fixed account ID returned by the WireMock internal API stub for the test user. */
  public static final String TEST_USER_ID = "00000000-0000-0000-0000-000000000001";

  /**
   * JDBC URL for the tasks PostgreSQL container. Used by {@code @QuarkusIntegrationTest} classes
   * for direct DB cleanup between tests (no {@code @Inject} available in integration test mode).
   */
  public static volatile String POSTGRES_JDBC_URL;

  private static GenericContainer<?> wireMock;
  private static GenericContainer<?> userManagement;
  private static PostgreSQLContainer<?> postgres;
  private static Network network;

  @Override
  public Map<String, String> start() {
    if (started) {
      return cachedConfig;
    }

    network = Network.newNetwork();

    wireMock =
        new GenericContainer<>("wiremock/wiremock:3.9.2")
            .withNetwork(network)
            .withNetworkAliases("carbonio-mailbox-mock", "consul")
            .withExposedPorts(8080)
            .waitingFor(
                Wait.forHttp("/__admin/health")
                    .forPort(8080)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    postgres =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName(DB_NAME)
            .withUsername(DB_USER)
            .withPassword(DB_PASSWORD);

    // WireMock and postgres have no inter-dependencies — start in parallel.
    Startables.deepStart(wireMock, postgres).join();

    // Configure WireMock BEFORE starting user-management:
    //   1. Register the REST stub for GET /internal/accounts/myself token validation.
    //   2. Register Consul HTTP API stubs so both tasks-ce and user-management can
    //      perform service discovery / KV lookups against WireMock on port 8080.
    try {
      String wireMockAdminUrl = "http://" + wireMock.getHost() + ":" + wireMock.getMappedPort(8080);
      setupMailboxWireMockStub(wireMockAdminUrl);
      setupConsulStubs(wireMockAdminUrl);
    } catch (Exception e) {
      throw new RuntimeException("Failed to configure WireMock stubs", e);
    }

    userManagement =
        new GenericContainer<>("registry.dev.zextras.com/dev/carbonio-user-management:devel")
            .withNetwork(network)
            .withNetworkAliases("carbonio-user-management")
            .withExposedPorts(10000)
            .withEnv("NETWORKING_CONFIG_CARBONIO_SERVICE_HOST", "0.0.0.0")
            .withEnv("NETWORKING_CONFIG_CARBONIO_SERVICE_PORT", "10000")
            .withEnv("NETWORKING_CONFIG_CARBONIO_SERVICE_DISCOVER_HOST", "consul")
            .withEnv("NETWORKING_CONFIG_CARBONIO_SERVICE_DISCOVER_PORT", "8080")
            // Point user-management at WireMock instead of a real mailbox (internal REST API)
            .withEnv("NETWORKING_CONFIG_CARBONIO_MAILBOX_INTERNAL_HOST", "carbonio-mailbox-mock")
            .withEnv("NETWORKING_CONFIG_CARBONIO_MAILBOX_INTERNAL_PORT", "8080")
            .dependsOn(wireMock)
            .waitingFor(
                Wait.forHttp("/q/health/live")
                    .forPort(10000)
                    .withStartupTimeout(Duration.ofMinutes(5)));

    userManagement.start();

    POSTGRES_JDBC_URL =
        String.format(
            "jdbc:postgresql://%s:%d/%s?sslmode=disable",
            postgres.getHost(), postgres.getFirstMappedPort(), DB_NAME);

    cachedConfig =
        Map.ofEntries(
            // Service identity
            Map.entry("networking-config.carbonio.service.host", "localhost"),
            // Consul (tasks-ce service discovery) → WireMock acting as consul
            Map.entry("networking-config.carbonio.service-discover.host", wireMock.getHost()),
            Map.entry(
                "networking-config.carbonio.service-discover.port",
                String.valueOf(wireMock.getMappedPort(8080))),
            // PostgreSQL (tasks database)
            Map.entry("networking-config.carbonio.postgresql.host", postgres.getHost()),
            Map.entry(
                "networking-config.carbonio.postgresql.port",
                String.valueOf(postgres.getFirstMappedPort())),
            Map.entry("quarkus.datasource.jdbc.url", POSTGRES_JDBC_URL),
            Map.entry("quarkus.datasource.username", DB_USER),
            Map.entry("quarkus.datasource.password", DB_PASSWORD),
            // User Management REST client
            Map.entry("networking-config.carbonio.user-management.host", "localhost"),
            Map.entry(
                "networking-config.carbonio.user-management.port",
                String.valueOf(userManagement.getMappedPort(10000))));
    started = true;
    return cachedConfig;
  }

  @Override
  public void stop() {
    // Containers are static singletons: they persist for the full test-run JVM lifetime.
    // Testcontainers' JVM shutdown hook will stop them when the JVM exits.
  }

  /**
   * Registers a WireMock stub for the mailbox internal REST endpoint that user-management calls to
   * validate auth tokens: {@code GET /internal/accounts/myself}.
   *
   * <p>When the Cookie header contains {@code ZM_AUTH_TOKEN=test-auth-token-tasks-ce}, WireMock
   * returns a minimal {@code AccountInfo} JSON. Any other token gets no stub → WireMock 404 →
   * user-management treats the token as invalid → tasks-ce returns 401.
   *
   * <p>The {@code features} map must include {@code carbonioFeatureTasksEnabled: true} — tasks-ce's
   * {@code AuthenticationFilter} checks this before granting access.
   */
  private static void setupMailboxWireMockStub(String wireMockAdminUrl) throws Exception {
    String stubJson =
        "{"
            + "\"request\":{"
            + "\"method\":\"GET\","
            + "\"urlPath\":\"/internal/accounts/myself\","
            + "\"headers\":{\"Cookie\":{\"contains\":\"ZM_AUTH_TOKEN="
            + AUTH_TOKEN
            + "\"}}"
            + "},"
            + "\"response\":{"
            + "\"status\":200,"
            + "\"headers\":{\"Content-Type\":\"application/json; charset=utf-8\"},"
            + "\"jsonBody\":{"
            + "\"id\":\""
            + TEST_USER_ID
            + "\","
            + "\"name\":\"test@carbonio.test\","
            + "\"displayName\":\"Test User\","
            + "\"status\":\"active\","
            + "\"isGlobalAdmin\":false,"
            // Both booleans, because mailbox really returns both and they are NOT synonyms:
            // isExternal is derived from zimbraMailTransport not matching the server named by
            // zimbraMailHost (foreign/relayed MTA routing), while isExternalVirtualAccount is the
            // LDAP zimbraIsExternalVirtualAccount flag marking a guest / external-share account.
            // user-management's UserService#mapAccountInfoToUserMyself classifies GUEST-vs-INTERNAL
            // off isExternalVirtualAccount() only. A normal internal account is false for both.
            // Omitting the field let the mailbox-sdk AccountInfo record silently default it to
            // false, which happened to give the right answer here only because this suite never
            // exercises a guest account.
            + "\"isExternal\":false,"
            + "\"isExternalVirtualAccount\":false,"
            + "\"locale\":\"en_US\","
            + "\"features\":{\"carbonioFeatureTasksEnabled\":true},"
            + "\"capabilities\":{},"
            + "\"sessionLifetimeMs\":86400000"
            + "}"
            + "}"
            + "}";

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(wireMockAdminUrl + "/__admin/mappings"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(stubJson))
            .build();

    HttpResponse<String> response =
        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 201) {
      throw new RuntimeException(
          "Failed to configure WireMock stub (HTTP "
              + response.statusCode()
              + "): "
              + response.body());
    }
  }

  /**
   * Registers WireMock stubs that impersonate the Consul HTTP API.
   *
   * <p>tasks-ce reads its DB credentials from Consul KV at startup. user-management reads optional
   * cache-TTL config (returns 404 → defaults used). Both services register themselves as Consul
   * services (→ 200 stubs).
   */
  private static void setupConsulStubs(String wireMockAdminUrl) throws Exception {
    HttpClient client = HttpClient.newHttpClient();

    // DB credentials for tasks-ce.
    // carbonio-quarkus-extensions (>= 1.10.x) issues a SINGLE ROOT recursive GET at boot:
    //   GET /v1/kv/?recurse   (prefix == "", urlPath ignores the query string)
    // Consul ACL-filters that root recurse to the keys the token can read; the boot factory then
    // derives the own-service application-config view from the carbonio-tasks/* subset. So we stub
    // the ROOT recurse (not the per-prefix one) and return all three credential entries in the
    // Consul recursive-response format. (Pre-1.10 the factory recursed /v1/kv/carbonio-tasks/.)
    postConsulKvRecursiveStub(
        client,
        wireMockAdminUrl,
        "",
        new String[][] {
          {"carbonio-tasks/database/credentials/db-name", DB_NAME},
          {"carbonio-tasks/database/credentials/db-username", DB_USER},
          {"carbonio-tasks/database/credentials/db-password", DB_PASSWORD},
        });

    // Catch-all for unknown KV keys → 404 (priority 10 = lowest; urlPathPattern ignores query)
    postStub(
        client,
        wireMockAdminUrl,
        "{\"priority\":10,"
            + "\"request\":{\"method\":\"GET\",\"urlPathPattern\":\"/v1/kv/.*\"},"
            + "\"response\":{\"status\":404}}");

    // Service registration / deregistration → 200
    for (String pattern :
        new String[] {
          "/v1/agent/service/register.*",
          "/v1/agent/service/deregister/.*",
          "/v1/agent/check/register.*",
          "/v1/agent/check/deregister/.*"
        }) {
      postStub(
          client,
          wireMockAdminUrl,
          "{\"request\":{\"method\":\"PUT\",\"urlPathPattern\":\""
              + pattern
              + "\"},"
              + "\"response\":{\"status\":200}}");
    }

    // Service discovery → empty array
    for (String pattern : new String[] {"/v1/health/service/.*", "/v1/catalog/service/.*"}) {
      postStub(
          client,
          wireMockAdminUrl,
          "{\"request\":{\"method\":\"GET\",\"urlPathPattern\":\""
              + pattern
              + "\"},"
              + "\"response\":{\"status\":200,"
              + "\"headers\":{\"Content-Type\":\"application/json\"},\"body\":\"[]\"}}");
    }

    // Agent self / status (urlPath = path-only exact match, ignores query string)
    postStub(
        client,
        wireMockAdminUrl,
        "{\"request\":{\"method\":\"GET\",\"urlPath\":\"/v1/agent/self\"},"
            + "\"response\":{\"status\":200,"
            + "\"headers\":{\"Content-Type\":\"application/json\"},"
            + "\"jsonBody\":{\"Config\":{\"Datacenter\":\"dc1\",\"NodeName\":\"mock-consul\"}}}}");
    postStub(
        client,
        wireMockAdminUrl,
        "{\"request\":{\"method\":\"GET\",\"urlPath\":\"/v1/status/leader\"},"
            + "\"response\":{\"status\":200,"
            + "\"headers\":{\"Content-Type\":\"application/json\"},"
            + "\"body\":\"\\\"127.0.0.1:8300\\\"\"}}");
  }

  /**
   * Registers a single WireMock stub that matches the Consul recursive KV fetch: GET
   * /v1/kv/{prefix}?recurse (urlPath ignores the query string)
   *
   * <p>CarbonioBootstrapFactory issues exactly one bulk GET — it never fetches individual keys. The
   * response is a JSON array with one object per key-value pair, values base64-encoded, which is
   * exactly what the real Consul API returns for {@code ?recurse}.
   *
   * @param kvEntries array of {key, plainTextValue} pairs to include in the response
   */
  private static void postConsulKvRecursiveStub(
      HttpClient client, String baseUrl, String prefix, String[][] kvEntries) throws Exception {
    StringBuilder arrayBody = new StringBuilder("[");
    for (int i = 0; i < kvEntries.length; i++) {
      String key = kvEntries[i][0];
      String value = kvEntries[i][1];
      String b64 = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
      if (i > 0) arrayBody.append(",");
      arrayBody
          .append("{\"LockIndex\":0,\"Key\":\"")
          .append(key)
          .append("\",\"Flags\":0,")
          .append("\"Value\":\"")
          .append(b64)
          .append("\",\"CreateIndex\":1,\"ModifyIndex\":1}");
    }
    arrayBody.append("]");

    // Escape the JSON array for embedding as a string value inside the WireMock stub JSON
    String escapedBody = arrayBody.toString().replace("\\", "\\\\").replace("\"", "\\\"");

    // urlPath matches /v1/kv/carbonio-tasks/ regardless of ?recurse or any other query param
    postStub(
        client,
        baseUrl,
        "{\"priority\":1,"
            + "\"request\":{\"method\":\"GET\",\"urlPath\":\"/v1/kv/"
            + prefix
            + "\"},"
            + "\"response\":{\"status\":200,"
            + "\"headers\":{\"Content-Type\":\"application/json\"},"
            + "\"body\":\""
            + escapedBody
            + "\"}}");
  }

  /** Posts a single WireMock stub JSON to the admin mappings endpoint. */
  private static void postStub(HttpClient client, String baseUrl, String stubJson)
      throws Exception {
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/__admin/mappings"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(stubJson))
            .build();
    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() != 201) {
      throw new RuntimeException(
          "Failed to register consul stub (HTTP " + resp.statusCode() + "): " + resp.body());
    }
  }
}
