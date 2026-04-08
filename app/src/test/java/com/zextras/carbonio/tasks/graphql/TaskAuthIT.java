// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql;

import com.zextras.carbonio.tasks.ConsulTestResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the AuthenticationFilter is correctly wired into the GraphQL endpoint.
 * Does NOT require MockUserManagementTestResource — the filter catches ALL StatusRuntimeException
 * (including UNAVAILABLE from connection refused) and returns 401.
 */
@QuarkusTest
@WithTestResource(ConsulTestResource.class)
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
        .cookie("ZM_AUTH_TOKEN", "not-a-real-token")
        .body(ANY_QUERY)
        .when()
        .post("/graphql")
        .then()
        .statusCode(401);
  }
}
