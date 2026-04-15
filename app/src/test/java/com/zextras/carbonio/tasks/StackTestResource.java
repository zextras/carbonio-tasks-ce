// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.io.InputStream;
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
 * <ul>
 *   <li>Direct dependencies of tasks-ce are run as real Docker containers:
 *       {@code carbonio-user-management} (gRPC auth validation).</li>
 *   <li>Indirect dependencies (dependencies of our direct deps) are replaced with
 *       lightweight mocks so that our IT suite is isolated from their failures.
 *       Specifically, {@code carbonio-mailbox} — which user-management calls for
 *       token validation — is stubbed by a WireMock SOAP server. mailbox's own
 *       integration with LDAP, MariaDB, and Postfix is covered by user-management's
 *       integration test suite, not ours.</li>
 *   <li>Consul is also stubbed by the same WireMock container (via a second network alias)
 *       so no real Consul container is needed.</li>
 * </ul>
 *
 * <p>Containers are static singletons: they start once per JVM and are reused
 * across all {@code @QuarkusIntegrationTest} classes. {@code stop()} is a no-op;
 * Testcontainers' JVM shutdown hook handles cleanup.
 */
public class StackTestResource implements QuarkusTestResourceLifecycleManager {

  private static final String DB_NAME = "carbonio-tasks-db";
  private static final String DB_USER = "test";
  private static final String DB_PASSWORD = "test";

  private static volatile boolean started = false;
  private static Map<String, String> cachedConfig;

  /**
   * Fixed {@code ZM_AUTH_TOKEN} used by tests and matched by the WireMock stub.
   * Any other token value will receive no stub match → WireMock returns 404 →
   * user-management treats the token as invalid → tasks-ce returns 401.
   */
  public static final String AUTH_TOKEN = "test-auth-token-tasks-ce";

  /** Fixed {@code zimbraId} returned by the WireMock SOAP stub for the test user. */
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
    //   1. Upload the WSDL and XSD schema files so user-management's JAX-WS client can
    //      parse the WSDL at http://{mailboxHost}/service/wsdl/ZimbraService.wsdl on boot.
    //   2. Register the SOAP stub for GetInfoRequest token validation.
    //   3. Register Consul HTTP API stubs so both tasks-ce and user-management can
    //      perform service discovery / KV lookups against WireMock on port 8080.
    try {
      String wireMockAdminUrl =
          "http://" + wireMock.getHost() + ":" + wireMock.getMappedPort(8080);
      uploadWsdlAndSchemas(wireMockAdminUrl);
      setupMailboxWireMockStub(wireMockAdminUrl);
      setupConsulStubs(wireMockAdminUrl);
    } catch (Exception e) {
      throw new RuntimeException("Failed to configure WireMock stubs", e);
    }

    userManagement =
        new GenericContainer<>(
                "registry.dev.zextras.com/dev/carbonio-user-management:devel")
            .withNetwork(network)
            .withNetworkAliases("carbonio-user-management")
            .withExposedPorts(10000) // gRPC and HTTP share port 10000 (use-separate-server=false)
            .withEnv("NETWORKING_CONFIG_CARBONIO_SERVICE_HOST", "0.0.0.0")
            .withEnv("NETWORKING_CONFIG_CARBONIO_SERVICE_PORT", "10000")
            .withEnv("NETWORKING_CONFIG_CARBONIO_SERVICE_DISCOVER_HOST", "consul")
            .withEnv("NETWORKING_CONFIG_CARBONIO_SERVICE_DISCOVER_PORT", "8080")
            // Point user-management at WireMock instead of a real mailbox
            .withEnv("NETWORKING_CONFIG_CARBONIO_MAILBOX_HOST", "carbonio-mailbox-mock")
            .withEnv("NETWORKING_CONFIG_CARBONIO_MAILBOX_PORT", "8080")
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
            // User Management gRPC client
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
   * Uploads the WSDL and XSD schema files to WireMock's {@code __files} directory so that
   * user-management's JAX-WS client can fetch them at startup.
   *
   * <p>WireMock serves any file under {@code __files} at the corresponding URL path. Files
   * uploaded via {@code PUT /__admin/files/{path}} are accessible at {@code GET /{path}}.
   *
   * <p>user-management's {@code MailboxClient.Builder} fetches the WSDL at startup from
   * {@code http://{mailbox}/service/wsdl/ZimbraService.wsdl}. The WSDL's {@code import}
   * elements reference the XSDs with relative paths (e.g. {@code zimbra.xsd}), so the JAX-WS
   * runtime resolves them at {@code http://{mailbox}/service/wsdl/zimbra.xsd} etc.
   *
   * <p>Schema files are bundled as test resources under {@code wsdl/} (extracted from
   * {@code carbonio-mailbox-sdk} 1.14.0, which matches the version compiled into the
   * {@code carbonio-user-management:devel} Docker image).
   */
  private static void uploadWsdlAndSchemas(String wireMockAdminUrl) throws Exception {
    HttpClient client = HttpClient.newHttpClient();

    String[] schemaFiles = {
        "ZimbraService.wsdl",
        "zimbra.xsd",
        "zimbraAccount.xsd",
        "zimbraMail.xsd",
        "zimbraAdmin.xsd"
    };

    for (String fileName : schemaFiles) {
      String resourcePath = "wsdl/" + fileName;
      byte[] content;
      try (InputStream is =
          Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
        if (is == null) {
          throw new RuntimeException(
              "Schema resource not found on classpath: " + resourcePath);
        }
        content = is.readAllBytes();
      }

      // Upload to WireMock's __files at service/wsdl/{fileName}
      // → served automatically at GET /service/wsdl/{fileName}
      HttpRequest uploadRequest =
          HttpRequest.newBuilder()
              .uri(URI.create(wireMockAdminUrl + "/__admin/files/service/wsdl/" + fileName))
              .header("Content-Type", "application/octet-stream")
              .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
              .build();

      HttpResponse<String> uploadResponse =
          client.send(uploadRequest, HttpResponse.BodyHandlers.ofString());

      if (uploadResponse.statusCode() != 200 && uploadResponse.statusCode() != 201) {
        throw new RuntimeException(
            "Failed to upload " + fileName + " to WireMock (HTTP "
                + uploadResponse.statusCode() + "): " + uploadResponse.body());
      }
    }
  }

  /**
   * Registers a WireMock stub that intercepts the SOAP {@code GetInfoRequest} call that
   * user-management sends to mailbox for token validation.
   *
   * <p>When a request arrives at {@code POST /service/soap} with a Cookie header containing
   * {@code ZM_AUTH_TOKEN=test-auth-token-tasks-ce}, WireMock returns a minimal but valid
   * SOAP 1.2 {@code GetInfoResponse}. Any other token gets no stub → 404 → UM treats the
   * token as invalid → tasks-ce returns 401.
   *
   * <p>The XML format matches the schema generated from {@code zimbraAccount.xsd} (from
   * carbonio-mailbox-sdk). Attributes use {@code <attr name="...">value</attr>} (not
   * {@code <a n="...">}). This format is confirmed by UM's own test resources at
   * {@code app/src/test/resources/soap/responses/GetInfoResponse.xml}.
   */
  private static void setupMailboxWireMockStub(String wireMockAdminUrl) throws Exception {
    // Minimal valid SOAP 1.2 GetInfoResponse — fields required by UserService.mapGetInfoToUserMyself:
    //   response.getName()       → <name>
    //   response.getPublicURL()  → <publicURL>
    //   response.getLifetime()   → <lifetime>
    //   attrs: zimbraId, displayName, zimbraAccountStatus, zimbraIsExternalVirtualAccount
    // Fields version, id, profileImageId are required by the XSD but can be dummy values.
    String soapResponseXml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">"
            + "<soap:Header>"
            + "<context xmlns=\"urn:zimbra\"><change token=\"1\"/></context>"
            + "</soap:Header>"
            + "<soap:Body>"
            + "<GetInfoResponse xmlns=\"urn:zimbraAccount\""
            + " docSizeLimit=\"10485760\" attSizeLimit=\"10240000\">"
            + "<version>23.9.0_ZEXTRAS_202309 carbonio 20230816-0759 FOSS</version>"
            + "<id>" + TEST_USER_ID + "</id>"
            + "<profileImageId>0</profileImageId>"
            + "<name>test@carbonio.test</name>"
            + "<lifetime>86400000</lifetime>"
            + "<prefs>"
            + "<pref name=\"zimbraPrefLocale\">en</pref>"
            + "</prefs>"
            + "<attrs>"
            + "<attr name=\"zimbraId\">" + TEST_USER_ID + "</attr>"
            + "<attr name=\"displayName\">Test User</attr>"
            + "<attr name=\"zimbraAccountStatus\">active</attr>"
            + "<attr name=\"zimbraIsExternalVirtualAccount\">FALSE</attr>"
            // carbonioFeatureTasksEnabled=TRUE → tasks-ce AuthenticationFilter grants access
            + "<attr name=\"carbonioFeatureTasksEnabled\">TRUE</attr>"
            + "</attrs>"
            + "<soapURL>http://carbonio.test/service/soap/</soapURL>"
            + "<publicURL>http://carbonio.test</publicURL>"
            + "</GetInfoResponse>"
            + "</soap:Body>"
            + "</soap:Envelope>";

    // Escape the XML for embedding inside a JSON string value
    String escapedXml = soapResponseXml
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");

    String stubJson =
        "{"
            + "\"request\": {"
            + "  \"method\": \"POST\","
            + "  \"url\": \"/service/soap/\","
            + "  \"headers\": {"
            + "    \"Cookie\": { \"contains\": \"ZM_AUTH_TOKEN=" + AUTH_TOKEN + "\" }"
            + "  }"
            + "},"
            + "\"response\": {"
            + "  \"status\": 200,"
            + "  \"headers\": { \"Content-Type\": \"application/soap+xml; charset=utf-8\" },"
            + "  \"body\": \"" + escapedXml + "\""
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
          "Failed to configure WireMock stub (HTTP " + response.statusCode() + "): "
              + response.body());
    }
  }

  /**
   * Registers WireMock stubs that impersonate the Consul HTTP API.
   *
   * <p>tasks-ce reads its DB credentials from Consul KV at startup.
   * user-management reads optional cache-TTL config (returns 404 → defaults used).
   * Both services register themselves as Consul services (→ 200 stubs).
   */
  private static void setupConsulStubs(String wireMockAdminUrl) throws Exception {
    HttpClient client = HttpClient.newHttpClient();

    // DB credentials for tasks-ce
    postConsulKvStub(client, wireMockAdminUrl, "carbonio-tasks/db-name",     DB_NAME);
    postConsulKvStub(client, wireMockAdminUrl, "carbonio-tasks/db-username", DB_USER);
    postConsulKvStub(client, wireMockAdminUrl, "carbonio-tasks/db-password", DB_PASSWORD);

    // Catch-all for unknown KV keys → 404 (priority 10 = lowest)
    postStub(client, wireMockAdminUrl,
        "{\"priority\":10,"
        + "\"request\":{\"method\":\"GET\",\"urlPattern\":\"/v1/kv/.*\"},"
        + "\"response\":{\"status\":404}}");

    // Service registration / deregistration → 200
    for (String pattern : new String[]{
        "/v1/agent/service/register.*",
        "/v1/agent/service/deregister/.*",
        "/v1/agent/check/register.*",
        "/v1/agent/check/deregister/.*"}) {
      postStub(client, wireMockAdminUrl,
          "{\"request\":{\"method\":\"PUT\",\"urlPattern\":\"" + pattern + "\"},"
          + "\"response\":{\"status\":200}}");
    }

    // Service discovery → empty array
    for (String pattern : new String[]{"/v1/health/service/.*", "/v1/catalog/service/.*"}) {
      postStub(client, wireMockAdminUrl,
          "{\"request\":{\"method\":\"GET\",\"urlPattern\":\"" + pattern + "\"},"
          + "\"response\":{\"status\":200,"
          + "\"headers\":{\"Content-Type\":\"application/json\"},\"body\":\"[]\"}}");
    }

    // Agent self / status
    postStub(client, wireMockAdminUrl,
        "{\"request\":{\"method\":\"GET\",\"url\":\"/v1/agent/self\"},"
        + "\"response\":{\"status\":200,"
        + "\"headers\":{\"Content-Type\":\"application/json\"},"
        + "\"body\":\"{\\\\\"Config\\\\\":{\\\\\"Datacenter\\\\\":\\\\\"dc1\\\\\","
        + "\\\\\"NodeName\\\\\":\\\\\"mock-consul\\\\\"}}\"}}");
    postStub(client, wireMockAdminUrl,
        "{\"request\":{\"method\":\"GET\",\"url\":\"/v1/status/leader\"},"
        + "\"response\":{\"status\":200,"
        + "\"headers\":{\"Content-Type\":\"application/json\"},"
        + "\"body\":\"\\\\\"127.0.0.1:8300\\\\\"\"}}");
  }

  /** Registers a Consul KV GET stub that returns value in Consul's JSON-array format. */
  private static void postConsulKvStub(
      HttpClient client, String baseUrl, String key, String value) throws Exception {
    String b64 = Base64.getEncoder()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    String body = "[{\"LockIndex\":0,\"Key\":\"" + key + "\",\"Flags\":0,"
        + "\"Value\":\"" + b64 + "\",\"CreateIndex\":1,\"ModifyIndex\":1}]";
    // Escape body string for embedding inside JSON "body" field value
    String escapedBody = body.replace("\\", "\\\\").replace("\"", "\\\"");
    postStub(client, baseUrl,
        "{\"priority\":1,"
        + "\"request\":{\"method\":\"GET\",\"url\":\"/v1/kv/" + key + "\"},"
        + "\"response\":{\"status\":200,"
        + "\"headers\":{\"Content-Type\":\"application/json\"},"
        + "\"body\":\"" + escapedBody + "\"}}");
  }

  /** Posts a single WireMock stub JSON to the admin mappings endpoint. */
  private static void postStub(HttpClient client, String baseUrl, String stubJson) throws Exception {
    HttpRequest req = HttpRequest.newBuilder()
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
