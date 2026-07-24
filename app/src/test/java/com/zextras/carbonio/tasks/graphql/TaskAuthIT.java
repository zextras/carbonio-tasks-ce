// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql;

import com.zextras.carbonio.tasks.StackTestResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the AuthenticationFilter is correctly wired into the GraphQL endpoint and enforces
 * authentication using the REAL carbonio-user-management REST service.
 *
 * <p>Uses {@code @QuarkusIntegrationTest} so the app runs as a separate process and connects to
 * the real user-management container over the network.
 */
@QuarkusIntegrationTest
@WithTestResource(StackTestResource.class)
class TaskAuthIT {

  private static final String ANY_QUERY =
      "{\"query\": \"mutation { createTask(newTask: {title: \\\"t\\\"}) { id } }\"}";

  @Test
  void graphqlRequestWithoutCookieShouldReturn401() {
    RestAssured.given()
        .contentType(ContentType.JSON)
        .body(ANY_QUERY)
        .when()
        .post("/graphql")
        .then()
        .statusCode(401);
  }

  @Test
  void graphqlRequestWithInvalidCookieShouldReturn401() {
    RestAssured.given()
        .contentType(ContentType.JSON)
        .cookie("ZM_AUTH_TOKEN", "not-a-real-token-" + System.nanoTime())
        .body(ANY_QUERY)
        .when()
        .post("/graphql")
        .then()
        .statusCode(401);
  }
}
