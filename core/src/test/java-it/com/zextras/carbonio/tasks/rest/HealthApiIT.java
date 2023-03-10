// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.rest;

import com.zextras.carbonio.tasks.Simulator;
import com.zextras.carbonio.tasks.Simulator.SimulatorBuilder;
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
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class HealthApiIT {

  private static Simulator simulator;
  private static LocalConnector localConnector;

  @BeforeAll
  static void init() {
    simulator =
        SimulatorBuilder.aSimulator()
            .init()
            .withDatabase()
            .withServiceDiscover()
            .withRestServlet()
            .build()
            .start();

    localConnector = simulator.getHttpLocalConnector();
  }

  @AfterAll
  static void cleanUpAll() {
    simulator.stopAll();
  }

  @Test
  public void givenAnHealthLiveRequestTheServiceShouldReturn204StatusCode() throws Exception {
    // Given
    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.GET.toString());
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setURI(("/rest/health/live/"));

    // When
    Response httpFields =
        HttpTester.parseResponse(HttpTester.from(localConnector.getResponse(request.generate())));

    // Then
    Assertions.assertThat(httpFields.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
    Assertions.assertThat(httpFields.getContent().isEmpty()).isTrue();
  }

  @Test
  public void givenAnHealthReadyRequestTheServiceShouldReturn204StatusCode() throws Exception {
    // Given
    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.GET.toString());
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setURI(("/rest/health/ready/"));

    // When
    Response httpFields =
        HttpTester.parseResponse(HttpTester.from(localConnector.getResponse(request.generate())));

    // Then
    Assertions.assertThat(httpFields.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
    Assertions.assertThat(httpFields.getContent().isEmpty()).isTrue();
  }

  @Disabled // This test will be enabled when the database will be attached to the project
  @Test
  public void givenAnHealthReadyRequestTheServiceShouldReturn500StatusCodeWhenDbConnectionFails()
      throws Exception {
    // Given
    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.GET.toString());
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setURI(("/rest/health/ready/"));

    // When
    Response httpFields =
        HttpTester.parseResponse(HttpTester.from(localConnector.getResponse(request.generate())));

    // Then
    Assertions.assertThat(httpFields.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR_500);
    Assertions.assertThat(httpFields.getContent().isEmpty()).isTrue();
  }
}
