// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql;

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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

public class ServiceInfoApiIT {

  private static Simulator simulator;
  private static LocalConnector localConnector;

  @BeforeAll
  static void initClass() throws Exception {
    simulator = SimulatorBuilder.aSimulator().init().withGraphQlServlet().build().start();
    localConnector = simulator.getHttpLocalConnector();
  }

  @AfterAll
  static void cleanUp() throws Exception {
    simulator.stopAll();
  }

  @Disabled // until all the schema type are bound to a data fetcher
  @Test
  public void givenGetProjectInfoQueryTheServiceShouldReturn200WithTheProjectInformation()
      throws Exception {
    // Given
    HttpTester.Request request = HttpTester.newRequest();
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setContent(TestUtils.queryPayload("query{getServiceInfo{name version}}"));

    // When
    Response response =
        HttpTester.parseResponse(HttpTester.from(localConnector.getResponse(request.generate())));

    // Then
    Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

    JSONAssert.assertEquals(
        "{\"data\":{\"getServiceInfo\":{\"name\":\"carbonio-tasks-ce\",\"version\":\"0.0.1\"}}}",
        response.getContent(),
        false);
  }
}