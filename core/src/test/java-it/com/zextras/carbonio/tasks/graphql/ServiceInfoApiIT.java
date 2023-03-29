// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql;

import com.google.common.collect.ImmutableMap;
import com.zextras.carbonio.tasks.Simulator;
import com.zextras.carbonio.tasks.Simulator.SimulatorBuilder;
import com.zextras.carbonio.tasks.TestUtils;
import org.assertj.core.api.Assertions;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.http.HttpTester.Response;
import org.eclipse.jetty.server.LocalConnector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

public class ServiceInfoApiIT {

  private static Simulator simulator;
  private static LocalConnector localConnector;

  @BeforeAll
  static void init() {
    simulator =
        SimulatorBuilder.aSimulator()
            .init()
            .withServer()
            .withUserManagement(
                ImmutableMap.<String, String>builder()
                    .put("fake-user-cookie", "00000000-0000-0000-0000-000000000000")
                    .build())
            .build()
            .start();
    localConnector = simulator.getHttpLocalConnector();
  }

  @AfterAll
  static void cleanUpAll() {
    simulator.stopAll();
  }

  @Test
  public void givenGetProjectInfoQueryTheServiceShouldReturn200WithTheProjectInformation()
      throws Exception {
    // Given
    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setHeader(HttpHeader.COOKIE.toString(), "ZM_AUTH_TOKEN=fake-user-cookie");
    request.setContent(TestUtils.queryPayload("query{getServiceInfo{name version flavour}}"));

    // When
    Response response =
        HttpTester.parseResponse(HttpTester.from(localConnector.getResponse(request.generate())));

    // Then
    Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

    JSONAssert.assertEquals(
        "{\"data\":{\"getServiceInfo\":{"
            + "\"name\":\"carbonio-tasks\","
            + "\"version\":\"0.0.1\","
            + "\"flavour\":\"community edition\""
            + "}}}",
        response.getContent(),
        false);
  }
}
