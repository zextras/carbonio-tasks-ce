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
 * <p>Uses {@code @QuarkusIntegrationTest} so the app runs as a separate process — this avoids
 * Quarkus test-mode gRPC routing and enables connecting to the real user-management container.
 * DB cleanup uses direct JDBC since {@code @Inject} is not available in integration test mode.
 */
@QuarkusIntegrationTest
@WithTestResource(StackTestResource.class)
class TasksGraphQLApiIT {

  @BeforeEach
  void cleanUp() throws Exception {
    try (var conn = DriverManager.getConnection(
             StackTestResource.POSTGRES_JDBC_URL, "test", "test");
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
        "{\"query\": \"{ getTask(taskId: \\\""
            + taskId
            + "\\\") { id title priority status } }\"}";

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
    String query =
        "{\"query\": \"{ getTask(taskId: \\\""
            + unknownId
            + "\\\") { id title } }\"}";

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
    createTaskViaApiWithStatus("Closed Task", "CLOSED");

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

    postAuth(
        "{\"query\": \"{ getTask(taskId: \\\""
            + taskId
            + "\\\") { id } }\"}")
        .statusCode(200)
        .body("errors", Matchers.notNullValue())
        .body(
            "errors[0].message",
            Matchers.containsString("Could not find task with id " + taskId));
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
        "{\"query\": \"mutation { createTask(newTask: {title: \\\""
            + title
            + "\\\"}) { id } }\"}";
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
