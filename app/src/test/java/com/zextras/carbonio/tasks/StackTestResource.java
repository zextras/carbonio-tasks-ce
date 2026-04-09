// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

import com.zextras.carbonio.quarkus.extensions.bootstrap.ConsulTestHelper;
import com.zextras.carbonio.quarkus.extensions.bootstrap.db.CarbonioDatabaseServiceConfig;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.jar.JarFile;
import org.testcontainers.consul.ConsulContainer;
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
  private static ConsulContainer consul;
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
            .withNetworkAliases("carbonio-mailbox-mock")
            .withExposedPorts(8080)
            .waitingFor(
                Wait.forHttp("/__admin/health")
                    .forPort(8080)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    consul =
        new ConsulContainer("hashicorp/consul:1.22.3")
            .withNetwork(network)
            .withNetworkAliases("consul");

    postgres =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName(DB_NAME)
            .withUsername(DB_USER)
            .withPassword(DB_PASSWORD);

    // WireMock, consul, and postgres have no inter-dependencies — start in parallel.
    Startables.deepStart(wireMock, consul, postgres).join();

    // Configure WireMock BEFORE starting user-management:
    //   1. Upload the WSDL and XSD schema files that user-management's JAX-WS client
    //      needs to parse when it boots (MailboxClient.Builder fetches the WSDL from
    //      http://{mailboxHost}/service/wsdl/ZimbraService.wsdl at startup).
    //   2. Register the SOAP stub for GetInfoRequest token validation.
    try {
      String wireMockAdminUrl =
          "http://" + wireMock.getHost() + ":" + wireMock.getMappedPort(8080);
      uploadWsdlAndSchemas(wireMockAdminUrl);
      setupMailboxWireMockStub(wireMockAdminUrl);
    } catch (Exception e) {
      throw new RuntimeException("Failed to configure WireMock mailbox stub", e);
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
            .withEnv("NETWORKING_CONFIG_CARBONIO_SERVICE_DISCOVER_PORT", "8500")
            // Point user-management at WireMock instead of a real mailbox
            .withEnv("NETWORKING_CONFIG_CARBONIO_MAILBOX_HOST", "carbonio-mailbox-mock")
            .withEnv("NETWORKING_CONFIG_CARBONIO_MAILBOX_PORT", "8080")
            .dependsOn(consul)
            .waitingFor(
                Wait.forHttp("/q/health/live")
                    .forPort(10000)
                    .withStartupTimeout(Duration.ofMinutes(5)));

    userManagement.start();

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

    cachedConfig =
        Map.ofEntries(
            // Service identity
            Map.entry("networking-config.carbonio.service.host", "localhost"),
            // Consul (tasks-ce service discovery)
            Map.entry("networking-config.carbonio.service-discover.host", consulHost),
            Map.entry(
                "networking-config.carbonio.service-discover.port", String.valueOf(consulPort)),
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
   * Uploads the WSDL and XSD schema files to WireMock's {@code __files} directory.
   *
   * <p>WireMock serves any file under {@code __files} at the corresponding URL path. Files
   * uploaded via {@code PUT /__admin/files/{path}} are accessible at {@code GET /{path}}.
   *
   * <p>user-management's {@code MailboxClient.Builder} fetches the WSDL at startup from
   * {@code http://{mailbox}/service/wsdl/ZimbraService.wsdl}. The WSDL's {@code import}
   * elements reference the XSDs with relative paths (e.g. {@code zimbra.xsd}), so the JAX-WS
   * runtime resolves them at {@code http://{mailbox}/service/wsdl/zimbra.xsd} etc.
   *
   * <p>Schema files are read from the {@code carbonio-mailbox-sdk} jar INSIDE the
   * user-management Docker image. This is critical: the WSDL must match the version of
   * {@code ZcsPortType} compiled into user-management's jar, or JAX-WS will throw a
   * {@code WebServiceException} for any method exposed in the interface but missing in the WSDL.
   * Using the WSDL from the Maven local repo risks a version drift if the Docker image was
   * built from a snapshot or from a newer/different SDK build.
   *
   * <p>The extraction uses {@code docker create} (no container start) + {@code docker cp}
   * to read the jar from the image layer without running the container.
   */
  private static void uploadWsdlAndSchemas(String wireMockAdminUrl) throws Exception {
    HttpClient client = HttpClient.newHttpClient();

    // Extract the carbonio-mailbox-sdk schemas from the UM Docker image.
    // The UM uber-jar (at /app/carbonio-user-management.jar) has the SDK bundled.
    File sdkJar = extractSdkJarFromImage();

    String[] schemaFiles = {
        "ZimbraService.wsdl",
        "zimbra.xsd",
        "zimbraAccount.xsd",
        "zimbraMail.xsd",
        "zimbraAdmin.xsd"
    };

    try (JarFile jarFile = new JarFile(sdkJar)) {
      for (String fileName : schemaFiles) {
        String entryPath = "schemas/" + fileName;
        var entry = jarFile.getEntry(entryPath);
        if (entry == null) {
          throw new RuntimeException(
              "Schema entry not found in SDK jar: " + entryPath + " (jar: " + sdkJar + ")");
        }

        byte[] content;
        try (InputStream is = jarFile.getInputStream(entry)) {
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
  }

  private static final String UM_IMAGE =
      "registry.dev.zextras.com/dev/carbonio-user-management:devel";
  private static final String UM_JAR_PATH = "/app/carbonio-user-management.jar";

  /**
   * Creates a temporary (non-started) container from the user-management image, copies the
   * uber-jar to the host filesystem, removes the temporary container, and returns the local
   * path to the jar.
   *
   * <p>Using {@code docker create} instead of {@code docker run} means no JVM starts, so the
   * operation is fast (a few seconds). The jar is written to a temp file that is deleted on JVM
   * exit.
   */
  private static File extractSdkJarFromImage() throws Exception {
    // Create a container (not started)
    Process createProc = new ProcessBuilder(
        "docker", "create", UM_IMAGE)
        .redirectErrorStream(true)
        .start();
    String createOutput = new String(createProc.getInputStream().readAllBytes()).trim();
    int createExit = createProc.waitFor();
    if (createExit != 0) {
      throw new RuntimeException(
          "docker create failed (exit " + createExit + "): " + createOutput);
    }
    String containerId = createOutput.lines().filter(l -> l.matches("[0-9a-f]{64}")).findFirst()
        .orElse(createOutput.trim()); // last line is the container ID

    try {
      // Copy the uber-jar out of the container
      File tempJar = File.createTempFile("um-sdk-", ".jar");
      tempJar.deleteOnExit();

      Process cpProc = new ProcessBuilder(
          "docker", "cp", containerId + ":" + UM_JAR_PATH, tempJar.getAbsolutePath())
          .redirectErrorStream(true)
          .start();
      String cpOutput = new String(cpProc.getInputStream().readAllBytes()).trim();
      int cpExit = cpProc.waitFor();
      if (cpExit != 0) {
        throw new RuntimeException(
            "docker cp failed (exit " + cpExit + "): " + cpOutput);
      }
      return tempJar;
    } finally {
      // Always remove the temporary container
      new ProcessBuilder("docker", "rm", containerId)
          .redirectErrorStream(true)
          .start()
          .waitFor();
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
}
