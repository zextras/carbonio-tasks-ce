// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.clients;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;
import org.testcontainers.shaded.com.trilead.ssh2.crypto.Base64;

class ServiceDiscoverHttpClientTest {

  private static ClientAndServer clientAndServer;
  private static MockServerClient serviceDiscoverMock;
  private static String bodyPayloadFormat;

  @BeforeAll
  static void init() {
    clientAndServer = ClientAndServer.startClientAndServer(8500, 9999);
    serviceDiscoverMock = new MockServerClient("localhost", 8500);
    bodyPayloadFormat = "[{\"Key\":\"%s\",\"Value\":\"%s\"}]";
  }

  @AfterAll
  static void cleanUpAll() {
    serviceDiscoverMock.stop();
    clientAndServer.stop();
  }

  @BeforeEach
  void setUp() {
    serviceDiscoverMock.reset();
  }

  @Test
  void givenAValidConfigKeyAndADefaultUrlTheGetConfigShouldReturnConfigValue() {
    // Given
    String encodedConfigValue =
        new String(Base64.encode("valid-value".getBytes(StandardCharsets.UTF_8)));

    serviceDiscoverMock
        .when(
            HttpRequest.request()
                .withPath("/v1/kv/carbonio-tasks/config-key")
                .withMethod(HttpMethod.GET.toString())
                .withHeader("X-Consul-Token", ""))
        .respond(
            HttpResponse.response()
                .withStatusCode(HttpStatus.OK_200)
                .withBody(String.format(bodyPayloadFormat, "config-key", encodedConfigValue)));

    // When
    Optional<String> optConfigValue =
        ServiceDiscoverHttpClient.defaultURL("carbonio-tasks").getConfig("config-key");

    // Then
    Assertions.assertThat(optConfigValue).isPresent().contains("valid-value");

    serviceDiscoverMock.verify(
        HttpRequest.request()
            .withPath("/v1/kv/carbonio-tasks/config-key")
            .withMethod(HttpMethod.GET.toString())
            .withHeader("X-Consul-Token", ""),
        VerificationTimes.once());
  }

  @Test
  void givenAValidConfigKeyAndACustomUrlTheGetConfigShouldReturnConfigValue() {
    // Given
    String encodedConfigValue =
        new String(Base64.encode("valid-value".getBytes(StandardCharsets.UTF_8)));

    MockServerClient customServiceDiscoverMock = new MockServerClient("localhost", 9999);
    customServiceDiscoverMock
        .when(
            HttpRequest.request()
                .withPath("/v1/kv/carbonio-tasks/config-key")
                .withMethod(HttpMethod.GET.toString())
                .withHeader("X-Consul-Token", ""))
        .respond(
            HttpResponse.response()
                .withStatusCode(HttpStatus.OK_200)
                .withBody(String.format(bodyPayloadFormat, "config-key", encodedConfigValue)));

    // When
    Optional<String> optConfigValue =
        ServiceDiscoverHttpClient.atURL("http://localhost:9999", "carbonio-tasks")
            .getConfig("config-key");

    // Then
    Assertions.assertThat(optConfigValue).isPresent().contains("valid-value");

    serviceDiscoverMock.verify(
        HttpRequest.request()
            .withPath("/v1/kv/carbonio-tasks/config-key")
            .withMethod(HttpMethod.GET.toString())
            .withHeader("X-Consul-Token", ""),
        VerificationTimes.once());

    customServiceDiscoverMock.reset();
  }

  @Test
  void givenAnInvalidConfigKeyAndADefaultUrlTheGetConfigShouldReturnAnEmptyOptional() {
    // Given
    serviceDiscoverMock
        .when(
            HttpRequest.request()
                .withPath("/v1/kv/carbonio-tasks/invalid-config-key")
                .withMethod(HttpMethod.GET.toString())
                .withHeader("X-Consul-Token", ""))
        .respond(HttpResponse.response().withStatusCode(HttpStatus.NOT_FOUND_404).withBody(""));

    // When
    Optional<String> optConfigValue =
        ServiceDiscoverHttpClient.defaultURL("carbonio-tasks").getConfig("invalid-config-key");

    // Then
    Assertions.assertThat(optConfigValue).isEmpty();

    serviceDiscoverMock.verify(
        HttpRequest.request()
            .withPath("/v1/kv/carbonio-tasks/invalid-config-key")
            .withMethod(HttpMethod.GET.toString())
            .withHeader("X-Consul-Token", ""),
        VerificationTimes.once());
  }

  @Test
  void givenAMalformedBodyResponseTheGetConfigShouldReturnAnEmptyOptional() {
    // Given
    serviceDiscoverMock
        .when(
            HttpRequest.request()
                .withPath("/v1/kv/carbonio-tasks/config-key")
                .withMethod(HttpMethod.GET.toString())
                .withHeader("X-Consul-Token", ""))
        .respond(
            HttpResponse.response()
                .withStatusCode(HttpStatus.OK_200)
                .withBody("invalid-payload-response"));

    // When
    Optional<String> optConfigValue =
        ServiceDiscoverHttpClient.defaultURL("carbonio-tasks").getConfig("config-key");

    // Then
    Assertions.assertThat(optConfigValue).isEmpty();

    serviceDiscoverMock.verify(
        HttpRequest.request()
            .withPath("/v1/kv/carbonio-tasks/config-key")
            .withMethod(HttpMethod.GET.toString())
            .withHeader("X-Consul-Token", ""),
        VerificationTimes.once());
  }

  @Test
  void givenAnUnreachableServiceDiscoverTheGetConfigShouldReturnAnEmptyOptional() {
    // Given
    serviceDiscoverMock
        .when(
            HttpRequest.request()
                .withPath("/v1/kv/carbonio-tasks/config-key")
                .withMethod(HttpMethod.GET.toString())
                .withHeader("X-Consul-Token", ""))
        .respond(HttpResponse.response().withStatusCode(HttpStatus.INTERNAL_SERVER_ERROR_500));

    // When
    Optional<String> optConfigValue =
        ServiceDiscoverHttpClient.defaultURL("carbonio-tasks").getConfig("config-key");

    // Then
    Assertions.assertThat(optConfigValue).isEmpty();

    serviceDiscoverMock.verify(
        HttpRequest.request()
            .withPath("/v1/kv/carbonio-tasks/config-key")
            .withMethod(HttpMethod.GET.toString())
            .withHeader("X-Consul-Token", ""),
        VerificationTimes.once());
  }

  @Test
  void givenAMalformedServiceDiscoverUrlTheGetConfigShouldThrownNullPointerException() {
    // Given
    serviceDiscoverMock
        .when(
            HttpRequest.request()
                .withPath("/v1/kv/carbonio-tasks/config-key")
                .withMethod(HttpMethod.GET.toString())
                .withHeader("X-Consul-Token", ""))
        .respond(HttpResponse.response().withStatusCode(HttpStatus.OK_200));

    // When
    ThrowableAssert.ThrowingCallable throwable =
        () -> ServiceDiscoverHttpClient.atURL("fake-url", "carbonio-tasks").getConfig("config-key");

    // Then
    Assertions.assertThatNullPointerException().isThrownBy(throwable);

    serviceDiscoverMock.verify(
        HttpRequest.request()
            .withPath("/v1/kv/carbonio-tasks/invalid-config-key")
            .withMethod(HttpMethod.GET.toString())
            .withHeader("X-Consul-Token", ""),
        VerificationTimes.never());
  }
}
