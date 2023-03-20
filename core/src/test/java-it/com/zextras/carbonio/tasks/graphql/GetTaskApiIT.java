// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql;

import com.google.common.collect.ImmutableMap;
import com.zextras.carbonio.tasks.Simulator;
import com.zextras.carbonio.tasks.Simulator.SimulatorBuilder;
import com.zextras.carbonio.tasks.TestUtils;
import com.zextras.carbonio.tasks.dal.dao.Priority;
import com.zextras.carbonio.tasks.dal.dao.Status;
import com.zextras.carbonio.tasks.dal.dao.Task;
import com.zextras.carbonio.tasks.dal.repositories.TaskRepository;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.server.LocalConnector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GetTaskApiIT {

  static Simulator simulator;
  static LocalConnector httpLocalConnector;
  static TaskRepository taskRepository;

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
            .withGraphQlServlet()
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
  public void givenAnExistingTaskIdTheGetTaskShouldReturnTheRequestedTask() throws Exception {
    // Given
    taskRepository.createTask(
        "00000000-0000-0000-0000-000000000000",
        "title1",
        null,
        Priority.HIGH,
        Status.OPEN,
        null,
        null);

    Task task =
        taskRepository.createTask(
            "00000000-0000-0000-0000-000000000000",
            "title2",
            "description",
            Priority.LOW,
            Status.OPEN,
            null,
            null);

    taskRepository.createTask(
        "11111111-1111-1111-1111-111111111111",
        "task",
        null,
        Priority.LOW,
        Status.OPEN,
        null,
        null);

    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setHeader(HttpHeader.COOKIE.toString(), "ZM_AUTH_TOKEN=fake-user-cookie");
    request.setContent(
        TestUtils.queryPayload(
            "query{getTask(taskId: \\\""
                + task.getId().toString()
                + "\\\"){"
                + "id title description priority status createdAt reminderAt reminderAllDay"
                + "}}"));

    // When
    HttpTester.Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Map<String, Object> requestedTask =
        TestUtils.jsonResponseToMap(response.getContent(), "getTask");

    Assertions.assertThat(requestedTask.get("id")).isEqualTo(task.getId().toString());
    Assertions.assertThat(requestedTask.get("title")).isEqualTo("title2");
    Assertions.assertThat(requestedTask.get("description")).isNotNull();
    Assertions.assertThat(requestedTask.get("description")).isEqualTo("description");
    Assertions.assertThat(requestedTask.get("priority")).isEqualTo("LOW");
    Assertions.assertThat(requestedTask.get("status")).isEqualTo("OPEN");
    Assertions.assertThat(requestedTask.get("createdAt"))
        .isEqualTo(task.getCreatedAt().toEpochMilli());
    Assertions.assertThat(requestedTask.get("reminderAt")).isNull();
    Assertions.assertThat(requestedTask.get("reminderAllDay")).isNull();
  }

  @Test
  public void givenANonExistingTaskIdTheGetTaskShouldReturn200CodeWithAnErrorMessage()
      throws Exception {
    // Given
    taskRepository.createTask(
        "11111111-1111-1111-1111-111111111111",
        "task",
        null,
        Priority.LOW,
        Status.OPEN,
        null,
        null);

    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setHeader(HttpHeader.COOKIE.toString(), "ZM_AUTH_TOKEN=fake-user-cookie");
    request.setContent(
        TestUtils.queryPayload(
            "query{getTask(taskId: \\\"6d162bee-3186-1111-bf31-59746a41600e\\\"){"
                + "id title description priority status createdAt reminderAt reminderAllDay"
                + "}}"));

    // When
    HttpTester.Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Map<String, Object> requestedTask =
        TestUtils.jsonResponseToMap(response.getContent(), "getTask");

    Assertions.assertThat(requestedTask.size()).isEqualTo(0);

    List<String> errors = TestUtils.jsonResponseToErrors(response.getContent());

    Assertions.assertThat(errors.size()).isEqualTo(1);
    Assertions.assertThat(errors.get(0))
        .isEqualTo("Could not find task with id 6d162bee-3186-1111-bf31-59746a41600e");
  }

  @Test
  public void givenAnExistingTaskIdOfAnotherUserTheGetTaskShouldReturn200CodeWithAnErrorMessage()
      throws Exception {
    // Given
    Task task =
        taskRepository.createTask(
            "11111111-1111-1111-1111-111111111111",
            "task",
            null,
            Priority.LOW,
            Status.OPEN,
            null,
            null);

    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setHeader(HttpHeader.COOKIE.toString(), "ZM_AUTH_TOKEN=fake-user-cookie");
    request.setContent(
        TestUtils.queryPayload(
            "query{getTask(taskId: \\\""
                + task.getId()
                + "\\\"){"
                + "id title description priority status createdAt reminderAt reminderAllDay"
                + "}}"));

    // When
    HttpTester.Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Map<String, Object> requestedTask =
        TestUtils.jsonResponseToMap(response.getContent(), "getTask");

    Assertions.assertThat(requestedTask.size()).isEqualTo(0);

    List<String> errors = TestUtils.jsonResponseToErrors(response.getContent());

    Assertions.assertThat(errors.size()).isEqualTo(1);
    Assertions.assertThat(errors.get(0)).isEqualTo("Could not find task with id " + task.getId());
  }
}
