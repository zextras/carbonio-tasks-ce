// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql;

import com.zextras.carbonio.tasks.ConsulTestResource;
import com.zextras.carbonio.tasks.Constants.Tasks;
import com.zextras.carbonio.tasks.MockUserManagementTestResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Integration test for the {@code getServiceInfo} GraphQL query.
 *
 * <p>All GraphQL endpoints require authentication (enforced by the Vert.x route filter). This test
 * uses MockUserManagementTestResource to provide a valid fake token.
 */
@QuarkusTest
@WithTestResource(ConsulTestResource.class)
@WithTestResource(MockUserManagementTestResource.class)
class ServiceInfoApiIT {

  @Test
  void getServiceInfoShouldReturnCorrectServiceMetadata() {
    String query = "{\"query\": \"{ getServiceInfo { name version flavour } }\"}";

    RestAssured.given()
        .contentType(ContentType.JSON)
        .cookie("ZM_AUTH_TOKEN", MockUserManagementTestResource.FAKE_TOKEN)
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
