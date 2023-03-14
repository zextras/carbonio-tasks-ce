// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql;

import com.zextras.carbonio.tasks.Simulator;
import com.zextras.carbonio.tasks.Simulator.SimulatorBuilder;
import com.zextras.carbonio.tasks.TestUtils;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.http.HttpTester.Response;
import org.eclipse.jetty.server.LocalConnector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CreateTaskApiIT {

  static Simulator simulator;
  static LocalConnector httpLocalConnector;
  static String string1024chars =
      "drmOaBQ4v5xzTnbRdqt7PdUgK49oV5CstdsskBAnMEoJUypa410nWWGeoGFW5mv"
          + "mVMCjPjmCNAXww9Ptjj3ywNdmIV8cJbFT7ueOKLwaOgGucFuhhRCfXUHN4YCmxFJ4yfST67Amc8x5R2tJU2pHfgmLOwSN"
          + "r5xjogSsSvzfDTjVs5ohj2PK9zjl75mX7fWKZpYvLS1SkMGFWCUvYH5GnH9CP6aG8SwhIGH8Kmk0yNPvtDm9NTUMszj4C"
          + "p5Vuhwm78sqnlo2gZESyAeDd2O3HbSKm98CUrwfk4xHwq9LuRoTgD6WQk21LLTSv1j4GoQvElIHYcL0pdTEFEj2LUdRob"
          + "we1k3cKlR5J0iL52UEdUeLyeV6dVvPg5UTFbudNZm0urVouCFIC1bPvyhq7to7VHAfVVZFKJzCdNIqnAiVXR3hZgLnSrG"
          + "bjdA5et5d5W3cMXeN7WhMmQqpF6xacMLHclNWqjZkcPJs8tPEr89rBXn1UlaFiIU4QAXsuZZOXemUP3gyafKzSgaRZL2x"
          + "kcKT7vZuPkPBPxybQnV5CmGdI0zq5sZjU1BxIqxLH25g89Cf9xcGdoCW0nWzF1gp4EhBQ7Rc7wTNJYNcxdw5c8lUqLMrC"
          + "1c815Tmi9tIxFxjANqNSbocmWbUcqV5jtpJEY7oyZYcUW5JxFcUwyackjslB05XWPd5Rcb0gqdd72QEvyAh6vKUdNjymw"
          + "k19Eqk6Cs67Qx8yhcyLKoXZmJqTBCJ1NTYgP3ziMuvphC71phsGt8pO0V7hjoDBdmJH2zrvqxULJyKMsj5sHMEnFFtSyZ"
          + "WDxMHHA9yepuv33HIqV9zuC5LcYmKSyjtwj1yC1us3r2RPwNWZyFf6nV1ppwDjrIPZZ7UEjAjpH42WBgrGO9FHwfJz3CS"
          + "40DuRFXTHhWjwYXa3lzFkzaD734vOl0zLlKs7taKDWEAsCof04MyuYaOREVt9P9X14utNkgLI9wr9zWPLDOqQtYlGo4N2"
          + "ZBaFs607fCWOfMw6KZqwfP33gHFwE0a";

  @BeforeAll
  static void init() {
    simulator =
        SimulatorBuilder.aSimulator()
            .init()
            .withDatabase()
            .withServiceDiscover()
            .withGraphQlServlet()
            .build()
            .start();

    httpLocalConnector = simulator.getHttpLocalConnector();
  }

  @AfterAll
  static void cleanUpAll() {
    simulator.stopAll();
  }

  @Test
  public void givenACompleteNewTaskInputTheCreateTaskShouldCreateANewTask() throws Exception {
    // Given
    HttpTester.Request request = HttpTester.newRequest();
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setContent(
        TestUtils.queryPayload(
            "mutation { createTask(newTask: { "
                + "title: \\\"Real title\\\","
                + "description: \\\"super-description\\\","
                + "priority: HIGH,"
                + "status: COMPLETE,"
                + "reminderAt: 50,"
                + "reminderAllDay: true"
                + "}) {"
                + "id title description priority status createdAt reminderAt reminderAllDay"
                + "}}"));

    // When
    Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Map<String, Object> createdTask =
        TestUtils.jsonResponseToMap(response.getContent(), "createTask");

    Assertions.assertThat(createdTask.get("id")).isNotNull();
    Assertions.assertThat(createdTask.get("title")).isEqualTo("Real title");
    Assertions.assertThat(createdTask.get("description")).isNotNull();
    Assertions.assertThat(createdTask.get("description")).isEqualTo("super-description");
    Assertions.assertThat(createdTask.get("priority")).isEqualTo("HIGH");
    Assertions.assertThat(createdTask.get("status")).isEqualTo("COMPLETE");
    Assertions.assertThat(createdTask.get("createdAt")).isNotNull();
    Assertions.assertThat(createdTask.get("reminderAt")).isNotNull();
    Assertions.assertThat(createdTask.get("reminderAt")).isEqualTo(50);
    Assertions.assertThat(createdTask.get("reminderAllDay")).isNotNull();
    Assertions.assertThat(createdTask.get("reminderAllDay")).isEqualTo(Boolean.TRUE);
  }

  @Test
  public void givenOnlyATitleTheCreateTaskShouldCreateANewTask() throws Exception {
    // Given
    HttpTester.Request request = HttpTester.newRequest();
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setContent(
        TestUtils.queryPayload(
            "mutation { createTask(newTask: { "
                + "title: \\\"Real title\\\""
                + "}) {"
                + "id title description priority status createdAt reminderAt reminderAllDay"
                + "}}"));

    // When
    Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Map<String, Object> createdTask =
        TestUtils.jsonResponseToMap(response.getContent(), "createTask");

    Assertions.assertThat(createdTask.get("id")).isNotNull();
    Assertions.assertThat(createdTask.get("title")).isEqualTo("Real title");
    Assertions.assertThat(createdTask.get("description")).isNull();
    Assertions.assertThat(createdTask.get("priority")).isEqualTo("MEDIUM");
    Assertions.assertThat(createdTask.get("status")).isEqualTo("OPEN");
    Assertions.assertThat(createdTask.get("createdAt")).isNotNull();
    Assertions.assertThat(createdTask.get("reminderAt")).isNull();
    Assertions.assertThat(createdTask.get("reminderAllDay")).isNull();
  }

  @Test
  public void givenATitleExceedingTheMaxLengthTheCreateTaskShouldReturnAnError() throws Exception {
    // Given
    String longTitle = string1024chars + "a";
    HttpTester.Request request = HttpTester.newRequest();
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setContent(
        TestUtils.queryPayload(
            "mutation { createTask(newTask: { " + "title: \\\"" + longTitle + "\\\"" + "}) {id}}"));

    // When
    Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Map<String, Object> createdTask =
        TestUtils.jsonResponseToMap(response.getContent(), "createTask");
    Assertions.assertThat(createdTask).isEmpty();

    List<String> errors = TestUtils.jsonResponseToErrors(response.getContent());
    Assertions.assertThat(errors.size()).isEqualTo(1);
    Assertions.assertThat(errors.get(0))
        .isEqualTo("Invalid title. Length is more than 1024 characters");
  }

  @Test
  public void givenADescriptionExceedingTheMaxLengthTheCreateTaskShouldReturnAnError()
      throws Exception {
    // Given
    String longDescription =
        string1024chars + string1024chars + string1024chars + string1024chars + "a";
    HttpTester.Request request = HttpTester.newRequest();
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setContent(
        TestUtils.queryPayload(
            "mutation { createTask(newTask: { "
                + "title: \\\"title\\\", description: \\\""
                + longDescription
                + "\\\"}) {id}}"));

    // When
    Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Map<String, Object> createdTask =
        TestUtils.jsonResponseToMap(response.getContent(), "createTask");
    Assertions.assertThat(createdTask).isEmpty();

    List<String> errors = TestUtils.jsonResponseToErrors(response.getContent());
    Assertions.assertThat(errors.size()).isEqualTo(1);
    Assertions.assertThat(errors.get(0))
        .isEqualTo("Invalid description. Length is more than 4096 characters");
  }

  @Test
  public void givenAReminderAllDayWithoutAReminderAtTheCreateTaskShouldReturnAnError()
      throws Exception {
    // Given
    HttpTester.Request request = HttpTester.newRequest();
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setContent(
        TestUtils.queryPayload(
            "mutation { createTask(newTask: { "
                + "title: \\\"title\\\", reminderAllDay: true"
                + "}) {id}}"));

    // When
    Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Map<String, Object> createdTask =
        TestUtils.jsonResponseToMap(response.getContent(), "createTask");
    Assertions.assertThat(createdTask).isEmpty();

    List<String> errors = TestUtils.jsonResponseToErrors(response.getContent());
    Assertions.assertThat(errors.size()).isEqualTo(1);
    Assertions.assertThat(errors.get(0))
        .isEqualTo("The reminderAllDay attribute must be set only when the reminderAt is set");
  }
}
