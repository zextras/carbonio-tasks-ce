// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.tasks.Simulator;
import com.zextras.carbonio.tasks.Simulator.SimulatorBuilder;
import com.zextras.carbonio.tasks.rest.types.health.DependencyType;
import com.zextras.carbonio.tasks.rest.types.health.HealthStatus;
import com.zextras.carbonio.tasks.rest.types.health.ServiceHealth;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.http.HttpTester.Response;
import org.eclipse.jetty.server.LocalConnector;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

public class HealthApiIT {

  @Test
  public void
      givenAllDependenciesHealthyTheHealthShouldReturn200CodeWithTheHealthStatusOfEachDependency()
          throws Exception {
    // Given
    SimulatorBuilder simulatorBuilder =
        SimulatorBuilder.aSimulator()
            .init()
            .withDatabase()
            .withServiceDiscover()
            .withUserManagement(Collections.emptyMap())
            .withServer();

    try (Simulator simulator = simulatorBuilder.build().start()) {

      LocalConnector localConnector = simulator.getHttpLocalConnector();
      MockServerClient userManagementServiceMock = simulator.getUserManagementMock();

      userManagementServiceMock
          .when(HttpRequest.request().withMethod(HttpMethod.GET.toString()).withPath("/health/"))
          .respond(HttpResponse.response().withStatusCode(HttpStatus.OK_200));

      HttpTester.Request request = HttpTester.newRequest();
      request.setMethod(HttpMethod.GET.toString());
      request.setHeader(HttpHeader.HOST.toString(), "test");
      request.setURI(("/rest/health/"));

      // When
      Response httpFields =
          HttpTester.parseResponse(HttpTester.from(localConnector.getResponse(request.generate())));

      // Then
      Assertions.assertThat(httpFields.getStatus()).isEqualTo(HttpStatus.OK_200);

      HealthStatus healthStatus =
          new ObjectMapper().readValue(httpFields.getContent(), HealthStatus.class);
      Assertions.assertThat(healthStatus.isReady()).isTrue();
      List<ServiceHealth> dependenciesHealth = healthStatus.getDependencies();
      Assertions.assertThat(dependenciesHealth.size()).isEqualTo(2);

      Assertions.assertThat(dependenciesHealth.get(0).getName()).isEqualTo("database");
      Assertions.assertThat(dependenciesHealth.get(0).isLive()).isTrue();
      Assertions.assertThat(dependenciesHealth.get(0).isReady()).isTrue();
      Assertions.assertThat(dependenciesHealth.get(0).getType()).isEqualTo(DependencyType.REQUIRED);

      Assertions.assertThat(dependenciesHealth.get(1).getName())
          .isEqualTo("carbonio-user-management");
      Assertions.assertThat(dependenciesHealth.get(1).isLive()).isTrue();
      Assertions.assertThat(dependenciesHealth.get(1).isReady()).isTrue();
      Assertions.assertThat(dependenciesHealth.get(1).getType()).isEqualTo(DependencyType.REQUIRED);
    }
  }

  @Test
  public void
      givenUserManagementUnreachableTheHealthShouldReturn502CodeWithTheHealthStatusOfEachDependency()
          throws Exception {
    // Given
    // Notice tha absence of the UserManagement initialization
    SimulatorBuilder simulatorBuilder =
        SimulatorBuilder.aSimulator().init().withDatabase().withServiceDiscover().withServer();

    try (Simulator simulator = simulatorBuilder.build().start()) {
      LocalConnector localConnector = simulator.getHttpLocalConnector();

      HttpTester.Request request = HttpTester.newRequest();
      request.setMethod(HttpMethod.GET.toString());
      request.setHeader(HttpHeader.HOST.toString(), "test");
      request.setURI(("/rest/health/"));

      // When
      Response httpFields =
          HttpTester.parseResponse(HttpTester.from(localConnector.getResponse(request.generate())));

      // Then
      Assertions.assertThat(httpFields.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY_502);

      HealthStatus healthStatus =
          new ObjectMapper().readValue(httpFields.getContent(), HealthStatus.class);
      Assertions.assertThat(healthStatus.isReady()).isFalse();
      List<ServiceHealth> dependenciesHealth = healthStatus.getDependencies();
      Assertions.assertThat(dependenciesHealth.size()).isEqualTo(2);

      Assertions.assertThat(dependenciesHealth.get(0).getName()).isEqualTo("database");
      Assertions.assertThat(dependenciesHealth.get(0).isLive()).isTrue();
      Assertions.assertThat(dependenciesHealth.get(0).isReady()).isTrue();
      Assertions.assertThat(dependenciesHealth.get(0).getType()).isEqualTo(DependencyType.REQUIRED);

      Assertions.assertThat(dependenciesHealth.get(1).getName())
          .isEqualTo("carbonio-user-management");
      Assertions.assertThat(dependenciesHealth.get(1).isLive()).isFalse();
      Assertions.assertThat(dependenciesHealth.get(1).isReady()).isFalse();
      Assertions.assertThat(dependenciesHealth.get(1).getType()).isEqualTo(DependencyType.REQUIRED);
    }
  }

  @Test
  public void givenAnHealthServiceTheHealthLiveShouldReturn204StatusCode() throws Exception {
    // Given
    try (Simulator simulator = SimulatorBuilder.aSimulator().init().withServer().build().start()) {
      LocalConnector localConnector = simulator.getHttpLocalConnector();

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
  }

  @Test
  public void givenAllDependenciesHealthyTheHealthReadyShouldReturn204StatusCode()
      throws Exception {
    // Given
    SimulatorBuilder simulatorBuilder =
        SimulatorBuilder.aSimulator()
            .init()
            .withDatabase()
            .withServiceDiscover()
            .withUserManagement(Collections.emptyMap())
            .withServer();

    try (Simulator simulator = simulatorBuilder.build().start()) {

      LocalConnector localConnector = simulator.getHttpLocalConnector();
      MockServerClient userManagementServiceMock = simulator.getUserManagementMock();

      userManagementServiceMock
          .when(HttpRequest.request().withMethod(HttpMethod.GET.toString()).withPath("/health/"))
          .respond(HttpResponse.response().withStatusCode(HttpStatus.OK_200));

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
  }

  @Test
  public void givenUserManagementUnreachableTheHealthReadyShouldReturn502StatusCode()
      throws Exception {
    // Given
    // Notice tha absence of the UserManagement initialization
    SimulatorBuilder simulatorBuilder =
        SimulatorBuilder.aSimulator().init().withDatabase().withServiceDiscover().withServer();

    try (Simulator simulator = simulatorBuilder.build().start()) {
      LocalConnector localConnector = simulator.getHttpLocalConnector();

      HttpTester.Request request = HttpTester.newRequest();
      request.setMethod(HttpMethod.GET.toString());
      request.setHeader(HttpHeader.HOST.toString(), "test");
      request.setURI(("/rest/health/ready/"));

      // When
      Response httpFields =
          HttpTester.parseResponse(HttpTester.from(localConnector.getResponse(request.generate())));

      // Then
      Assertions.assertThat(httpFields.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY_502);
      Assertions.assertThat(httpFields.getContent().isEmpty()).isTrue();
    }
  }
}
