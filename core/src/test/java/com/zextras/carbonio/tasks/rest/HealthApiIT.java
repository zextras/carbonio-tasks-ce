// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.rest;

import com.zextras.carbonio.tasks.Constants.Service.API.Endpoints;
import org.assertj.core.api.Assertions;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.http.HttpTester.Response;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

@Testcontainers
public class HealthApiIT {

  private static Server server;
  private static LocalConnector localConnector;
  private static PostgreSQLContainer postgreSQLContainer;

  @BeforeAll
  static void setup() throws Exception {
    postgreSQLContainer =
        (PostgreSQLContainer)
            new PostgreSQLContainer("postgres:12.14")
                .withCopyFileToContainer(
                    MountableFile.forClasspathResource("/sql/postgresql_1.sql"),
                    "/docker-entrypoint-initdb.d/init.sql");
    postgreSQLContainer.start();

    server = new Server();
    localConnector = new LocalConnector(server);
    server.addConnector(localConnector);

    ServletContextHandler servletContextHandler = new ServletContextHandler();
    servletContextHandler.setContextPath(Endpoints.REST);
    ServletHolder servletHolder =
        servletContextHandler.addServlet(HttpServlet30Dispatcher.class, "/*");
    servletHolder.setInitParameter("javax.ws.rs.Application", RestApplication.class.getName());

    server.setHandler(servletContextHandler);
    server.start();
  }

  @AfterAll
  static void stop() throws Exception {
    server.stop();
    postgreSQLContainer.stop();
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
