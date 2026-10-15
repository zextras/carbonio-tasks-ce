// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql;

import com.zextras.carbonio.tasks.StackTestResource;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/** Verifies that GraphQL introspection queries are blocked in the packaged app (prod profile). */
class IntrospectionApiIT extends AbstractTasksIT {

  @Test
  void schemaIntrospectionShouldBeRejected() {
    postAuth("{\"query\": \"{ __schema { queryType { name } } }\"}")
        .body("errors", Matchers.not(Matchers.empty()))
        .body("data.__schema", Matchers.nullValue());
  }

  @Test
  void typeIntrospectionShouldBeRejected() {
    postAuth("{\"query\": \"{ __type(name: \\\"Query\\\") { name } }\"}")
        .body("errors", Matchers.not(Matchers.empty()))
        .body("data.__type", Matchers.nullValue());
  }

  @Test
  void typenameShouldStillWork() {
    postAuth("{\"query\": \"{ __typename }\"}")
        .statusCode(200)
        .body("data.__typename", Matchers.equalTo("Query"));
  }

  @Test
  void regularQueryShouldStillWork() {
    postAuth("{\"query\": \"{ getServiceInfo { name } }\"}")
        .statusCode(200)
        .body("data.getServiceInfo.name", Matchers.notNullValue());
  }

  private ValidatableResponse postAuth(String body) {
    return RestAssured.given()
        .contentType(ContentType.JSON)
        .cookie("ZM_AUTH_TOKEN", StackTestResource.AUTH_TOKEN)
        .body(body)
        .when()
        .post("/graphql")
        .then();
  }
}
