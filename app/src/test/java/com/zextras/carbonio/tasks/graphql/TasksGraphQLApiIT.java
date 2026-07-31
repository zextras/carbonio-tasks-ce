// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql;

import com.zextras.carbonio.tasks.StackTestResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.sql.DriverManager;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for all authenticated GraphQL operations.
 *
 * <p>Uses {@code @QuarkusIntegrationTest} so the app runs as a separate process and connects to the
 * real user-management container over the network. DB cleanup uses direct JDBC since
 * {@code @Inject} is not available in integration test mode.
 */
@QuarkusIntegrationTest
@WithTestResource(StackTestResource.class)
class TasksGraphQLApiIT {

  @BeforeEach
  void cleanUp() throws Exception {
    try (var conn =
            DriverManager.getConnection(StackTestResource.POSTGRES_JDBC_URL, "test", "test");
        var stmt = conn.createStatement()) {
      stmt.execute("DELETE FROM task");
    }
  }

  // ─────────────── createTask ───────────────

  @Test
  void createTaskWithTitleOnlyShouldReturnTaskWithDefaults() {
    String query =
        "{\"query\": \"mutation { createTask(newTask: {title: \\\"My Task\\\"}) "
            + "{ id title description priority status createdAt reminderAt reminderAllDay } }\"}";

    postAuth(query)
        .statusCode(200)
        .body("data.createTask.id", Matchers.notNullValue())
        .body("data.createTask.title", Matchers.equalTo("My Task"))
        .body("data.createTask.description", Matchers.nullValue())
        .body("data.createTask.priority", Matchers.equalTo("MEDIUM"))
        .body("data.createTask.status", Matchers.equalTo("OPEN"))
        .body("data.createTask.createdAt", Matchers.notNullValue())
        .body("data.createTask.reminderAt", Matchers.nullValue())
        .body("data.createTask.reminderAllDay", Matchers.nullValue());
  }

  @Test
  void createTaskWithAllFieldsShouldReturnFullTask() {
    String query =
        "{\"query\": \"mutation { createTask(newTask: {"
            + "title: \\\"Full Task\\\", "
            + "description: \\\"A description\\\", "
            + "priority: HIGH, "
            + "status: OPEN, "
            + "reminderAt: 1700000000000, "
            + "reminderAllDay: false"
            + "}) { id title description priority status reminderAt reminderAllDay } }\"}";

    postAuth(query)
        .statusCode(200)
        .body("data.createTask.title", Matchers.equalTo("Full Task"))
        .body("data.createTask.description", Matchers.equalTo("A description"))
        .body("data.createTask.priority", Matchers.equalTo("HIGH"))
        .body("data.createTask.reminderAt", Matchers.notNullValue())
        .body("data.createTask.reminderAllDay", Matchers.equalTo(false));
  }

  @Test
  void createTaskWithTitleExceeding1024CharsShouldReturnGraphQLError() {
    String longTitle = "x".repeat(1025);
    String query =
        "{\"query\": \"mutation { createTask(newTask: {title: \\\""
            + longTitle
            + "\\\"}) { id } }\"}";

    postAuth(query)
        .statusCode(200)
        .body("errors", Matchers.notNullValue())
        .body("errors[0].message", Matchers.containsString("Invalid title"));
  }

  @Test
  void createTaskWithReminderAtButNoReminderAllDayShouldReturnGraphQLError() {
    String query =
        "{\"query\": \"mutation { createTask(newTask: {"
            + "title: \\\"Task\\\", reminderAt: 1700000000000"
            + "}) { id } }\"}";

    postAuth(query)
        .statusCode(200)
        .body("errors", Matchers.notNullValue())
        .body(
            "errors[0].message",
            Matchers.containsString("reminderAt and the reminderAllDay attributes must be"));
  }

  // ─────────────── getTask ───────────────

  @Test
  void getTaskShouldReturnExistingTask() {
    String taskId = createTaskViaApi("Find Me");

    String getQuery =
        "{\"query\": \"{ getTask(taskId: \\\"" + taskId + "\\\") { id title priority status } }\"}";

    postAuth(getQuery)
        .statusCode(200)
        .body("data.getTask.id", Matchers.equalTo(taskId))
        .body("data.getTask.title", Matchers.equalTo("Find Me"))
        .body("data.getTask.priority", Matchers.equalTo("MEDIUM"))
        .body("data.getTask.status", Matchers.equalTo("OPEN"));
  }

  @Test
  void getTaskWithUnknownIdShouldReturnGraphQLError() {
    String unknownId = "00000000-0000-0000-0000-000000000001";
    String query = "{\"query\": \"{ getTask(taskId: \\\"" + unknownId + "\\\") { id title } }\"}";

    postAuth(query)
        .statusCode(200)
        .body("errors", Matchers.notNullValue())
        .body(
            "errors[0].message",
            Matchers.containsString("Could not find task with id " + unknownId));
  }

  // ─────────────── findTasks ───────────────

  @Test
  void findTasksShouldReturnEmptyListWhenNoTasksExist() {
    postAuth("{\"query\": \"{ findTasks { id } }\"}")
        .statusCode(200)
        .body("data.findTasks", Matchers.hasSize(0));
  }

  @Test
  void findTasksShouldReturnAllNonTrashedTasks() {
    createTaskViaApi("Task A");
    createTaskViaApi("Task B");

    postAuth("{\"query\": \"{ findTasks { id title } }\"}")
        .statusCode(200)
        .body("data.findTasks", Matchers.hasSize(2))
        .body("data.findTasks.title", Matchers.containsInAnyOrder("Task A", "Task B"));
  }

  @Test
  void findTasksShouldFilterByStatus() {
    createTaskViaApiWithStatus("Open Task", "OPEN");
    createTaskViaApiWithStatus("Completed Task", "COMPLETE");

    postAuth("{\"query\": \"{ findTasks(status: OPEN) { id title status } }\"}")
        .statusCode(200)
        .body("data.findTasks", Matchers.hasSize(1))
        .body("data.findTasks[0].title", Matchers.equalTo("Open Task"))
        .body("data.findTasks[0].status", Matchers.equalTo("OPEN"));
  }

  @Test
  void findTasksShouldFilterByPriority() {
    createTaskViaApiWithPriority("High Task", "HIGH");
    createTaskViaApiWithPriority("Low Task", "LOW");

    postAuth("{\"query\": \"{ findTasks(priority: HIGH) { id title priority } }\"}")
        .statusCode(200)
        .body("data.findTasks", Matchers.hasSize(1))
        .body("data.findTasks[0].title", Matchers.equalTo("High Task"))
        .body("data.findTasks[0].priority", Matchers.equalTo("HIGH"));
  }

  @Test
  void findTasksShouldExcludeTrashedTasks() {
    String taskId = createTaskViaApi("Task to Trash");
    postAuth("{\"query\": \"mutation { trashTask(taskId: \\\"" + taskId + "\\\") }\"}");

    postAuth("{\"query\": \"{ findTasks { id } }\"}")
        .statusCode(200)
        .body("data.findTasks", Matchers.hasSize(0));
  }

  // ─────────────── updateTask ───────────────

  @Test
  void updateTaskShouldUpdateTitle() {
    String taskId = createTaskViaApi("Original Title");

    String updateQuery =
        "{\"query\": \"mutation { updateTask(updateTask: {"
            + "id: \\\""
            + taskId
            + "\\\", "
            + "title: \\\"Updated Title\\\""
            + "}) { id title priority status } }\"}";

    postAuth(updateQuery)
        .statusCode(200)
        .body("data.updateTask.id", Matchers.equalTo(taskId))
        .body("data.updateTask.title", Matchers.equalTo("Updated Title"))
        .body("data.updateTask.priority", Matchers.equalTo("MEDIUM"))
        .body("data.updateTask.status", Matchers.equalTo("OPEN"));
  }

  @Test
  void updateTaskShouldSetAndClearReminder() {
    String taskId = createTaskViaApi("Reminder Task");

    // Set reminder
    postAuth(
            "{\"query\": \"mutation { updateTask(updateTask: {"
                + "id: \\\""
                + taskId
                + "\\\", reminderAt: 1700000000000, reminderAllDay: true"
                + "}) { id reminderAt reminderAllDay } }\"}")
        .statusCode(200)
        .body("data.updateTask.reminderAt", Matchers.notNullValue())
        .body("data.updateTask.reminderAllDay", Matchers.equalTo(true));

    // Clear reminder: reminderAt=0 (REMINDER_AT_RESET_VALUE) + reminderAllDay=false
    postAuth(
            "{\"query\": \"mutation { updateTask(updateTask: {"
                + "id: \\\""
                + taskId
                + "\\\", reminderAt: 0, reminderAllDay: false"
                + "}) { id reminderAt reminderAllDay } }\"}")
        .statusCode(200)
        .body("data.updateTask.reminderAt", Matchers.nullValue())
        .body("data.updateTask.reminderAllDay", Matchers.nullValue());
  }

  @Test
  void updateTaskWithUnknownIdShouldReturnGraphQLError() {
    String unknownId = "00000000-0000-0000-0000-000000000001";
    String query =
        "{\"query\": \"mutation { updateTask(updateTask: {"
            + "id: \\\""
            + unknownId
            + "\\\", title: \\\"New Title\\\""
            + "}) { id } }\"}";

    postAuth(query)
        .statusCode(200)
        .body("errors", Matchers.notNullValue())
        .body(
            "errors[0].message",
            Matchers.containsString("Could not find task with id " + unknownId));
  }

  // ─────────────── trashTask ───────────────

  @Test
  void trashTaskShouldReturnTaskId() {
    String taskId = createTaskViaApi("Task to Trash");

    postAuth("{\"query\": \"mutation { trashTask(taskId: \\\"" + taskId + "\\\") }\"}")
        .statusCode(200)
        .body("data.trashTask", Matchers.equalTo(taskId));
  }

  @Test
  void trashTaskShouldMakeTaskInvisibleToGetTask() {
    String taskId = createTaskViaApi("Disappearing Task");
    postAuth("{\"query\": \"mutation { trashTask(taskId: \\\"" + taskId + "\\\") }\"}");

    postAuth("{\"query\": \"{ getTask(taskId: \\\"" + taskId + "\\\") { id } }\"}")
        .statusCode(200)
        .body("errors", Matchers.notNullValue())
        .body(
            "errors[0].message", Matchers.containsString("Could not find task with id " + taskId));
  }

  @Test
  void trashTaskWithUnknownIdShouldReturnGraphQLError() {
    String unknownId = "00000000-0000-0000-0000-000000000001";
    postAuth("{\"query\": \"mutation { trashTask(taskId: \\\"" + unknownId + "\\\") }\"}")
        .statusCode(200)
        .body("errors", Matchers.notNullValue())
        .body(
            "errors[0].message",
            Matchers.containsString("Could not find task with id " + unknownId));
  }

  // ─────────────── native resource-bundle regression guard (i18n.Scalars) ───────────────
  //
  // graphql-java loads a ResourceBundle named "i18n.Scalars" to build the human-readable
  // messages for enum- and built-in-scalar-coercion failures (see
  // graphql.scalar.CoercingUtil#i18nMsg, used by graphql.schema.GraphQLEnumType and by the
  // Graphql*Coercing classes for Int/Float/Boolean/String/ID). Under GraalVM native-image,
  // ResourceBundle.getBundle(...) throws MissingResourceException unless the bundle is
  // explicitly registered at build time. quarkus-smallrye-graphql upstream only registers
  // i18n.Validation and i18n.Parsing, never i18n.Scalars, so WITHOUT the explicit
  // "-H:IncludeResourceBundles=i18n.Scalars" native build arg (see application.properties,
  // quarkus.native.additional-build-args) any invalid enum literal or invalid built-in-scalar
  // literal in a request crashes with an unhandled MissingResourceException, surfacing to the
  // client as HTTP 500 with "data": null and no "errors" array, instead of a normal HTTP 200
  // GraphQL validation error.
  //
  // The two tests below are the ONLY regression coverage for that registration. Commit 8700920
  // added the native build arg fix, but in the very same commit changed
  // findTasksShouldFilterByStatus to stop sending the invalid literal "CLOSED" (replacing it
  // with the valid "COMPLETE"), which silently deleted the only test exercising the code path
  // the fix protects. Do not repeat that mistake:
  //  - Do NOT "simplify" or "fix" these tests by switching to a valid Status value or a
  //    correctly-typed argument. An invalid literal is the entire point: it is what forces
  //    graphql-java down the i18n.Scalars-dependent coercion path.
  //  - These tests only prove the registration is intact when the suite runs against the
  //    NATIVE binary. In JVM mode, ResourceBundle.getBundle("i18n.Scalars", ...) resolves
  //    normally from i18n/Scalars.properties on the classpath regardless of the native build
  //    arg, so a green result here in JVM mode does NOT prove the native registration works —
  //    it only proves graphql-java's ordinary (non-native) behavior, which was never broken.
  //    As of this writing, CI does not yet run the IT suite against the native binary
  //    (jenkins-lib-common PR #150 adds that, still open).

  /**
   * Sends the exact invalid enum literal ({@code Status(CLOSED)}) that {@code
   * findTasksShouldFilterByStatus} used to send before commit 8700920 replaced it with a valid one.
   * {@code Status} only defines {@code OPEN}, {@code COMPLETE} and {@code TRASH}, so {@code CLOSED}
   * forces graphql-java's {@code GraphQLEnumType} coercion to fail and build its error message from
   * the {@code i18n.Scalars} resource bundle.
   *
   * <p>See the section comment above: this must keep sending an invalid literal, and it is only a
   * real guard against the native resource-bundle registration when run against the native binary.
   */
  @Test
  void findTasksWithInvalidEnumLiteralShouldReturnGraphQLErrorNotServerError() {
    postAuth("{\"query\": \"{ findTasks(status: CLOSED) { id title status } }\"}")
        // Explicitly NOT a 500: an unregistered i18n.Scalars bundle under native-image would
        // throw MissingResourceException and surface here as HTTP 500 with a null "data".
        .statusCode(200)
        .body("errors", Matchers.not(Matchers.empty()))
        .body("errors[0].message", Matchers.containsString("Status"));
  }

  /**
   * Sends a Boolean literal ({@code true}) for {@code taskId}, which the schema types as the
   * built-in {@code ID} scalar. {@code ID} only accepts {@code StringValue}/{@code IntValue}
   * literals, so this forces {@code GraphqlIDCoercing} to fail and build its error message from the
   * same {@code i18n.Scalars} bundle as the enum case above, via a different coercion path
   * (built-in scalar rather than enum).
   *
   * <p>See the section comment above: this must keep sending a wrongly-typed literal, and it is
   * only a real guard against the native resource-bundle registration when run against the native
   * binary.
   */
  @Test
  void getTaskWithWrongLiteralTypeShouldReturnGraphQLErrorNotServerError() {
    postAuth("{\"query\": \"{ getTask(taskId: true) { id } }\"}")
        // Explicitly NOT a 500: an unregistered i18n.Scalars bundle under native-image would
        // throw MissingResourceException and surface here as HTTP 500 with a null "data".
        .statusCode(200)
        .body("errors", Matchers.not(Matchers.empty()))
        .body("errors[0].message", Matchers.containsString("ID"));
  }

  // ─────────────── helpers ───────────────

  private ValidatableResponse postAuth(String jsonBody) {
    return RestAssured.given()
        .contentType(ContentType.JSON)
        .cookie("ZM_AUTH_TOKEN", StackTestResource.AUTH_TOKEN)
        .body(jsonBody)
        .when()
        .post("/graphql")
        .then();
  }

  String createTaskViaApi(String title) {
    String query =
        "{\"query\": \"mutation { createTask(newTask: {title: \\\"" + title + "\\\"}) { id } }\"}";
    return RestAssured.given()
        .contentType(ContentType.JSON)
        .cookie("ZM_AUTH_TOKEN", StackTestResource.AUTH_TOKEN)
        .body(query)
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .extract()
        .path("data.createTask.id");
  }

  private String createTaskViaApiWithStatus(String title, String status) {
    String query =
        "{\"query\": \"mutation { createTask(newTask: {title: \\\""
            + title
            + "\\\", status: "
            + status
            + "}) { id } }\"}";
    return RestAssured.given()
        .contentType(ContentType.JSON)
        .cookie("ZM_AUTH_TOKEN", StackTestResource.AUTH_TOKEN)
        .body(query)
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .extract()
        .path("data.createTask.id");
  }

  private String createTaskViaApiWithPriority(String title, String priority) {
    String query =
        "{\"query\": \"mutation { createTask(newTask: {title: \\\""
            + title
            + "\\\", priority: "
            + priority
            + "}) { id } }\"}";
    return RestAssured.given()
        .contentType(ContentType.JSON)
        .cookie("ZM_AUTH_TOKEN", StackTestResource.AUTH_TOKEN)
        .body(query)
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .extract()
        .path("data.createTask.id");
  }
}
