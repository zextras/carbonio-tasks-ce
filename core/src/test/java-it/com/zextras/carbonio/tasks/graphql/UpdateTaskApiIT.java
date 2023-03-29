// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql;

import com.zextras.carbonio.tasks.Simulator;
import com.zextras.carbonio.tasks.Simulator.SimulatorBuilder;
import com.zextras.carbonio.tasks.TestUtils;
import com.zextras.carbonio.tasks.dal.dao.Priority;
import com.zextras.carbonio.tasks.dal.dao.Status;
import com.zextras.carbonio.tasks.dal.dao.Task;
import com.zextras.carbonio.tasks.dal.repositories.TaskRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.http.HttpTester.Response;
import org.eclipse.jetty.server.LocalConnector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

public class UpdateTaskApiIT {

  static Simulator simulator;
  static LocalConnector httpLocalConnector;
  static TaskRepository taskRepository;
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
            .withUserManagement(
                ImmutableMap.<String, String>builder()
                    .put("fake-user-cookie", "00000000-0000-0000-0000-000000000000")
                    .build())
            .withServer()
            .build()
            .start();

    httpLocalConnector = simulator.getHttpLocalConnector();
    taskRepository = simulator.getInjector().getInstance(TaskRepository.class);
  }

  @AfterEach
  public void cleanUp() {
    simulator.resetDatabase();
  }

  @AfterAll
  static void cleanUpAll() {
    simulator.stopAll();
  }

  @Test
  public void givenAnExistingTaskAndAllUpdatedFieldsTheUpdateTaskShouldReturnTheUpdatedTask()
      throws Exception {
    // Given
    Task task =
        taskRepository.createTask(
            "00000000-0000-0000-0000-000000000000",
            "Title",
            "Description",
            Priority.HIGH,
            Status.OPEN,
            Instant.ofEpochMilli(50),
            true);

    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setHeader(HttpHeader.COOKIE.toString(), "ZM_AUTH_TOKEN=fake-user-cookie");
    request.setContent(
        TestUtils.queryPayload(
            "mutation { updateTask(updateTask: { "
                + "id: \\\""
                + task.getId().toString()
                + "\\\","
                + "title: \\\"Updated title\\\","
                + "description: \\\"Updated description\\\","
                + "priority: LOW,"
                + "status: COMPLETE,"
                + "reminderAt: 100,"
                + "reminderAllDay: false"
                + "}) {"
                + "id title description priority status createdAt reminderAt reminderAllDay"
                + "}}"));

    // When
    Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Map<String, Object> updatedTask =
        TestUtils.jsonResponseToMap(response.getContent(), "updateTask");

    Assertions.assertThat(updatedTask.get("id")).isEqualTo(task.getId().toString());
    Assertions.assertThat(updatedTask.get("title")).isEqualTo("Updated title");
    Assertions.assertThat(updatedTask.get("description")).isNotNull();
    Assertions.assertThat(updatedTask.get("description")).isEqualTo("Updated description");
    Assertions.assertThat(updatedTask.get("priority")).isEqualTo("LOW");
    Assertions.assertThat(updatedTask.get("status")).isEqualTo("COMPLETE");
    Assertions.assertThat(updatedTask.get("createdAt"))
        .isEqualTo(task.getCreatedAt().toEpochMilli());
    Assertions.assertThat(updatedTask.get("reminderAt")).isNotNull();
    Assertions.assertThat(updatedTask.get("reminderAt")).isEqualTo(100);
    Assertions.assertThat(updatedTask.get("reminderAllDay")).isNotNull();
    Assertions.assertThat(updatedTask.get("reminderAllDay")).isEqualTo(Boolean.FALSE);

    List<String> errors = TestUtils.jsonResponseToErrors(response.getContent());
    Assertions.assertThat(errors).isEmpty();
  }

  @Test
  public void
      givenAnExistingTaskAndNoFieldsToUpdateTheUpdateTaskDataFetcherShouldReturnTheUntouchedTask()
          throws Exception {
    // Given
    Task task =
        taskRepository.createTask(
            "00000000-0000-0000-0000-000000000000",
            "Title",
            "Description",
            Priority.HIGH,
            Status.OPEN,
            Instant.ofEpochMilli(50),
            true);

    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setHeader(HttpHeader.COOKIE.toString(), "ZM_AUTH_TOKEN=fake-user-cookie");
    request.setContent(
        TestUtils.queryPayload(
            "mutation { updateTask(updateTask: { "
                + "id: \\\""
                + task.getId().toString()
                + "\\\""
                + "}) {"
                + "id title description priority status createdAt reminderAt reminderAllDay"
                + "}}"));

    // When
    Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Map<String, Object> updatedTask =
        TestUtils.jsonResponseToMap(response.getContent(), "updateTask");

    Assertions.assertThat(updatedTask.get("id")).isEqualTo(task.getId().toString());
    Assertions.assertThat(updatedTask.get("title")).isEqualTo("Title");
    Assertions.assertThat(updatedTask.get("description")).isNotNull();
    Assertions.assertThat(updatedTask.get("description")).isEqualTo("Description");
    Assertions.assertThat(updatedTask.get("priority")).isEqualTo("HIGH");
    Assertions.assertThat(updatedTask.get("status")).isEqualTo("OPEN");
    Assertions.assertThat(updatedTask.get("createdAt"))
        .isEqualTo(task.getCreatedAt().toEpochMilli());
    Assertions.assertThat(updatedTask.get("reminderAt")).isNotNull();
    Assertions.assertThat(updatedTask.get("reminderAt")).isEqualTo(50);
    Assertions.assertThat(updatedTask.get("reminderAllDay")).isNotNull();
    Assertions.assertThat(updatedTask.get("reminderAllDay")).isEqualTo(Boolean.TRUE);

    List<String> errors = TestUtils.jsonResponseToErrors(response.getContent());
    Assertions.assertThat(errors).isEmpty();
  }

  @Test
  public void givenANotExistingTaskTheUpdateTaskDataFetcherShouldReturn200CodeWithAnErrorMessage()
      throws Exception {
    // Given
    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setHeader(HttpHeader.COOKIE.toString(), "ZM_AUTH_TOKEN=fake-user-cookie");
    request.setContent(
        TestUtils.queryPayload(
            "mutation { updateTask(updateTask: { "
                + "id: \\\"00000000-0000-0000-0000-000000000000\\\""
                + "title: \\\"Updated title\\\""
                + "}) {"
                + "id title description priority status createdAt reminderAt reminderAllDay"
                + "}}"));

    // When
    Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Map<String, Object> updatedTask =
        TestUtils.jsonResponseToMap(response.getContent(), "updateTask");

    Assertions.assertThat(updatedTask).isEmpty();

    List<String> errors = TestUtils.jsonResponseToErrors(response.getContent());
    Assertions.assertThat(errors.size()).isEqualTo(1);
    Assertions.assertThat(errors.get(0))
        .isEqualTo("Could not find task with id 00000000-0000-0000-0000-000000000000");
  }

  @Test
  public void givenATitleTooLongTheUpdateTaskDataFetcherShouldReturn200CodeWithAnErrorMessage()
      throws Exception {
    // Given
    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setHeader(HttpHeader.COOKIE.toString(), "ZM_AUTH_TOKEN=fake-user-cookie");
    request.setContent(
        TestUtils.queryPayload(
            "mutation { updateTask(updateTask: { "
                + "id: \\\"00000000-0000-0000-0000-000000000000\\\""
                + "title: \\\""
                + string1024chars
                + "0\\\""
                + "}) {"
                + "id"
                + "}}"));

    // When
    Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Map<String, Object> updatedTask =
        TestUtils.jsonResponseToMap(response.getContent(), "updateTask");

    Assertions.assertThat(updatedTask).isEmpty();

    List<String> errors = TestUtils.jsonResponseToErrors(response.getContent());
    Assertions.assertThat(errors.size()).isEqualTo(1);
    Assertions.assertThat(errors.get(0))
        .isEqualTo("Invalid title. Length is more than 1024 characters");
  }

  @Test
  public void givenATitleOf1024CharsTheUpdateTaskDataFetcherShouldReturnTheUpdatedTask()
      throws Exception {
    // Given
    Task task =
        taskRepository.createTask(
            "00000000-0000-0000-0000-000000000000",
            "Title",
            "Description",
            Priority.HIGH,
            Status.OPEN,
            Instant.ofEpochMilli(50),
            true);

    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setHeader(HttpHeader.COOKIE.toString(), "ZM_AUTH_TOKEN=fake-user-cookie");
    request.setContent(
        TestUtils.queryPayload(
            "mutation { updateTask(updateTask: { "
                + "id: \\\""
                + task.getId().toString()
                + "\\\""
                + "title: \\\""
                + string1024chars
                + "\\\""
                + "}) {"
                + "id title"
                + "}}"));

    // When
    Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Map<String, Object> updatedTask =
        TestUtils.jsonResponseToMap(response.getContent(), "updateTask");

    Assertions.assertThat(updatedTask.get("id")).isEqualTo(task.getId().toString());
    Assertions.assertThat(updatedTask.get("title")).isEqualTo(string1024chars);

    List<String> errors = TestUtils.jsonResponseToErrors(response.getContent());
    Assertions.assertThat(errors).isEmpty();
  }

  @Test
  public void
      givenADescriptionTooLongTheUpdateTaskDataFetcherShouldReturn200CodeWithAnErrorMessage()
          throws Exception {
    // Given
    String description =
        string1024chars + string1024chars + string1024chars + string1024chars + "1";
    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setHeader(HttpHeader.COOKIE.toString(), "ZM_AUTH_TOKEN=fake-user-cookie");
    request.setContent(
        TestUtils.queryPayload(
            "mutation { updateTask(updateTask: { "
                + "id: \\\"00000000-0000-0000-0000-000000000000\\\""
                + "description: \\\""
                + description
                + "\\\""
                + "}) {"
                + "id"
                + "}}"));

    // When
    Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Map<String, Object> updatedTask =
        TestUtils.jsonResponseToMap(response.getContent(), "updateTask");

    Assertions.assertThat(updatedTask).isEmpty();

    List<String> errors = TestUtils.jsonResponseToErrors(response.getContent());
    Assertions.assertThat(errors.size()).isEqualTo(1);
    Assertions.assertThat(errors.get(0))
        .isEqualTo("Invalid description. Length is more than 4096 characters");
  }

  @Test
  public void givenADescriptionOf4096CharsTheUpdateTaskDataFetcherShouldReturnTheUpdatedTask()
      throws Exception {
    // Given
    String description = string1024chars + string1024chars + string1024chars + string1024chars;
    Task task =
        taskRepository.createTask(
            "00000000-0000-0000-0000-000000000000",
            "Title",
            "Description",
            Priority.HIGH,
            Status.OPEN,
            Instant.ofEpochMilli(50),
            true);

    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setHeader(HttpHeader.COOKIE.toString(), "ZM_AUTH_TOKEN=fake-user-cookie");
    request.setContent(
        TestUtils.queryPayload(
            "mutation { updateTask(updateTask: { "
                + "id: \\\""
                + task.getId().toString()
                + "\\\""
                + "description: \\\""
                + description
                + "\\\""
                + "}) {"
                + "id description"
                + "}}"));

    // When
    Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Map<String, Object> updatedTask =
        TestUtils.jsonResponseToMap(response.getContent(), "updateTask");

    Assertions.assertThat(updatedTask.get("id")).isEqualTo(task.getId().toString());
    Assertions.assertThat(updatedTask.get("description")).isEqualTo(description);

    List<String> errors = TestUtils.jsonResponseToErrors(response.getContent());
    Assertions.assertThat(errors).isEmpty();
  }

  @Test
  public void givenOnlyAReminderAtTheUpdateTaskDataFetcherShouldReturn200CodeWithAnErrorMessage()
      throws Exception {
    // Given
    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setHeader(HttpHeader.COOKIE.toString(), "ZM_AUTH_TOKEN=fake-user-cookie");
    request.setContent(
        TestUtils.queryPayload(
            "mutation { updateTask(updateTask: { "
                + "id: \\\"00000000-0000-0000-0000-000000000000\\\""
                + "reminderAt: 5"
                + "}) {"
                + "id"
                + "}}"));

    // When
    Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Map<String, Object> updatedTask =
        TestUtils.jsonResponseToMap(response.getContent(), "updateTask");

    Assertions.assertThat(updatedTask).isEmpty();

    List<String> errors = TestUtils.jsonResponseToErrors(response.getContent());
    Assertions.assertThat(errors.size()).isEqualTo(1);
    Assertions.assertThat(errors.get(0))
        .isEqualTo("The reminderAt and the reminderAllDay attributes must be both always set");
  }

  @Test
  public void
      givenOnlyAReminderAllDayTheUpdateTaskDataFetcherShouldReturn200CodeWithAnErrorMessage()
          throws Exception {
    // Given
    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setHeader(HttpHeader.COOKIE.toString(), "ZM_AUTH_TOKEN=fake-user-cookie");
    request.setContent(
        TestUtils.queryPayload(
            "mutation { updateTask(updateTask: { "
                + "id: \\\"00000000-0000-0000-0000-000000000000\\\""
                + "reminderAllDay: true"
                + "}) {"
                + "id"
                + "}}"));

    // When
    Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Map<String, Object> updatedTask =
        TestUtils.jsonResponseToMap(response.getContent(), "updateTask");

    Assertions.assertThat(updatedTask).isEmpty();

    List<String> errors = TestUtils.jsonResponseToErrors(response.getContent());
    Assertions.assertThat(errors.size()).isEqualTo(1);
    Assertions.assertThat(errors.get(0))
        .isEqualTo("The reminderAt and the reminderAllDay attributes must be both always set");
  }

  @Test
  public void
      givenAnExistingTaskAndReminderAtToZeroTheUpdateTaskDataFetcherShouldReturnTheTaskWithTheReminderReset()
          throws Exception {
    // Given
    Task task =
        taskRepository.createTask(
            "00000000-0000-0000-0000-000000000000",
            "Title",
            "Description",
            Priority.HIGH,
            Status.OPEN,
            Instant.ofEpochMilli(50),
            true);

    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setHeader(HttpHeader.COOKIE.toString(), "ZM_AUTH_TOKEN=fake-user-cookie");
    request.setContent(
        TestUtils.queryPayload(
            "mutation { updateTask(updateTask: { "
                + "id: \\\""
                + task.getId().toString()
                + "\\\","
                + "reminderAt: 0,"
                + "reminderAllDay: false"
                + "}) {"
                + "id title description priority status createdAt reminderAt reminderAllDay"
                + "}}"));

    // When
    Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Map<String, Object> updatedTask =
        TestUtils.jsonResponseToMap(response.getContent(), "updateTask");

    Assertions.assertThat(updatedTask.get("id")).isEqualTo(task.getId().toString());
    Assertions.assertThat(updatedTask.get("reminderAt")).isNull();
    Assertions.assertThat(updatedTask.get("reminderAllDay")).isNull();

    List<String> errors = TestUtils.jsonResponseToErrors(response.getContent());
    Assertions.assertThat(errors).isEmpty();
  }
}
