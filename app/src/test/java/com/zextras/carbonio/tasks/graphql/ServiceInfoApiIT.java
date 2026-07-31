// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql;

import com.zextras.carbonio.tasks.Constants.Tasks;
import com.zextras.carbonio.tasks.StackTestResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/** Integration test for the {@code getServiceInfo} GraphQL query. */
@QuarkusIntegrationTest
@WithTestResource(StackTestResource.class)
class ServiceInfoApiIT {

  @Test
  void getServiceInfoShouldReturnCorrectServiceMetadata() {
    String query = "{\"query\": \"{ getServiceInfo { name version flavour } }\"}";

    RestAssured.given()
        .contentType(ContentType.JSON)
        .cookie("ZM_AUTH_TOKEN", StackTestResource.AUTH_TOKEN)
        .body(query)
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.getServiceInfo.name", Matchers.equalTo(Tasks.SERVICE_NAME))
        .body("data.getServiceInfo.version", Matchers.equalTo(Tasks.VERSION))
        .body("data.getServiceInfo.flavour", Matchers.equalTo(Tasks.FLAVOUR));
  }
}
