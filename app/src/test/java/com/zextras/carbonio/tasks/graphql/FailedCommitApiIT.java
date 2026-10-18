// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.zextras.carbonio.tasks.StackTestResource;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A resolver whose commit fails answers the generic error and leaves the database untouched. A
 * deferred trigger refuses the commit of any task titled {@value #REFUSED_TITLE}.
 */
class FailedCommitApiIT extends AbstractTasksIT {

  private static final String REFUSED_TITLE = "refused-at-commit";

  @BeforeEach
  void refuseTheCommitOfTheRefusedTitle() throws SQLException {
    execute("DELETE FROM task");
    execute(
        "CREATE OR REPLACE FUNCTION refuse_commit() RETURNS trigger AS $$ BEGIN"
            + " IF NEW.title = '"
            + REFUSED_TITLE
            + "' THEN RAISE EXCEPTION 'commit refused'; END IF;"
            + " RETURN NEW; END $$ LANGUAGE plpgsql");
    execute(
        "CREATE CONSTRAINT TRIGGER refuse_commit AFTER INSERT OR UPDATE ON task"
            + " DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION refuse_commit()");
  }

  @AfterEach
  void dropTheTrigger() throws SQLException {
    execute("DROP TRIGGER IF EXISTS refuse_commit ON task");
    execute("DROP FUNCTION IF EXISTS refuse_commit()");
  }

  @Test
  void aCreateTaskWhoseCommitFailsAnswersTheGenericErrorAndCreatesNothing() throws SQLException {
    createTask("Original");

    postAuth(
            "{\"query\": \"mutation { createTask(newTask: {title: \\\""
                + REFUSED_TITLE
                + "\\\"}) { id } }\"}")
        .statusCode(200)
        .body("data.createTask", Matchers.nullValue())
        .body("errors[0].message", Matchers.equalTo("System error"));

    assertThat(tasks()).containsExactly("Original MEDIUM");
  }

  @Test
  void anUpdateTaskWhoseCommitFailsAnswersTheGenericErrorAndChangesNothing() throws SQLException {
    String taskId = createTask("Original");

    postAuth(
            "{\"query\": \"mutation { updateTask(updateTask: {id: \\\""
                + taskId
                + "\\\", title: \\\""
                + REFUSED_TITLE
                + "\\\", priority: HIGH}) { id } }\"}")
        .statusCode(200)
        .body("data.updateTask", Matchers.nullValue())
        .body("errors[0].message", Matchers.equalTo("System error"));

    assertThat(tasks()).containsExactly("Original MEDIUM");
  }

  private static String createTask(String title) {
    return postAuth(
            "{\"query\": \"mutation { createTask(newTask: {title: \\\""
                + title
                + "\\\"}) { id } }\"}")
        .statusCode(200)
        .body("data.createTask.id", Matchers.notNullValue())
        .extract()
        .path("data.createTask.id");
  }

  private static ValidatableResponse postAuth(String jsonBody) {
    return RestAssured.given()
        .contentType(ContentType.JSON)
        .cookie("ZM_AUTH_TOKEN", StackTestResource.AUTH_TOKEN)
        .body(jsonBody)
        .when()
        .post("/graphql")
        .then();
  }

  private static List<String> tasks() throws SQLException {
    List<String> tasks = new ArrayList<>();
    try (Connection conn = openConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT title, priority FROM task")) {
      while (rs.next()) {
        tasks.add(rs.getString("title") + " " + rs.getString("priority"));
      }
    }
    return tasks;
  }

  private static void execute(String sql) throws SQLException {
    try (Connection conn = openConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
    }
  }

  private static Connection openConnection() throws SQLException {
    return DriverManager.getConnection(StackTestResource.POSTGRES_JDBC_URL, "test", "test");
  }
}
