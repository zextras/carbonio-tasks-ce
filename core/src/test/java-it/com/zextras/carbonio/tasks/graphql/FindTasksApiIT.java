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
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.http.HttpTester.Response;
import org.eclipse.jetty.server.LocalConnector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FindTasksApiIT {

  private static Simulator simulator;
  private static LocalConnector httpLocalConnector;
  private static TaskRepository taskRepository;

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

  @AfterAll
  static void cleanUpAll() {
    simulator.stopAll();
  }

  @AfterEach
  void cleanUp() {
    simulator.resetDatabase();
  }

  @Test
  void givenAnOpenStatusTheFindTasksShouldReturnTheTasksOfTheRequesterInAOpenState()
      throws Exception {
    // Given
    Task task1 =
        taskRepository.createTask(
            "00000000-0000-0000-0000-000000000000",
            "title1",
            null,
            Priority.MEDIUM,
            Status.OPEN,
            null,
            null);

    Task task2 =
        taskRepository.createTask(
            "00000000-0000-0000-0000-000000000000",
            "title2",
            null,
            Priority.LOW,
            Status.OPEN,
            null,
            null);

    taskRepository.createTask(
        "00000000-0000-0000-0000-000000000000",
        "title3",
        null,
        Priority.HIGH,
        Status.COMPLETE,
        null,
        null);

    taskRepository.createTask(
        "11111111-1111-1111-1111-111111111111",
        "titleX",
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
            "query{findTasks(status: OPEN){"
                + "id title description priority status createdAt reminderAt reminderAllDay"
                + "}}"));

    // When
    Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
    List<Map<String, Object>> findTasks =
        TestUtils.jsonResponseToList(response.getContent(), "findTasks");

    Assertions.assertThat(findTasks).isNotNull().hasSize(2);

    Map<String, Object> result1 = findTasks.get(0);
    Assertions.assertThat(result1)
        .containsEntry("id", task2.getId().toString())
        .containsEntry("title", "title2")
        .containsEntry("description", null)
        .containsEntry("priority", "LOW")
        .containsEntry("status", "OPEN")
        .containsEntry("createdAt", task2.getCreatedAt().toEpochMilli())
        .containsEntry("reminderAt", null)
        .containsEntry("reminderAllDay", null);

    Map<String, Object> result2 = findTasks.get(1);
    Assertions.assertThat(result2)
        .containsEntry("id", task1.getId().toString())
        .containsEntry("title", "title1")
        .containsEntry("description", null)
        .containsEntry("priority", "MEDIUM")
        .containsEntry("status", "OPEN")
        .containsEntry("createdAt", task1.getCreatedAt().toEpochMilli())
        .containsEntry("reminderAt", null)
        .containsEntry("reminderAllDay", null);
  }

  @Test
  void givenAnEmptyStatusTheFindTasksShouldReturnTheTasksOfTheRequesterInAOpenAndCompleteState()
      throws Exception {
    // Given
    Task task1 =
        taskRepository.createTask(
            "00000000-0000-0000-0000-000000000000",
            "title1",
            null,
            Priority.MEDIUM,
            Status.OPEN,
            null,
            null);

    Task task2 =
        taskRepository.createTask(
            "00000000-0000-0000-0000-000000000000",
            "title2",
            null,
            Priority.LOW,
            Status.OPEN,
            null,
            null);

    Task task3 =
        taskRepository.createTask(
            "00000000-0000-0000-0000-000000000000",
            "title3",
            null,
            Priority.HIGH,
            Status.COMPLETE,
            null,
            null);

    taskRepository.createTask(
        "11111111-1111-1111-1111-111111111111",
        "titleX",
        null,
        Priority.LOW,
        Status.COMPLETE,
        null,
        null);

    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setHeader(HttpHeader.COOKIE.toString(), "ZM_AUTH_TOKEN=fake-user-cookie");
    request.setContent(
        TestUtils.queryPayload(
            "query{findTasks {"
                + "id title description priority status createdAt reminderAt reminderAllDay"
                + "}}"));

    // When
    Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
    List<Map<String, Object>> findTasks =
        TestUtils.jsonResponseToList(response.getContent(), "findTasks");

    Assertions.assertThat(findTasks).isNotNull().hasSize(3);

    Map<String, Object> result1 = findTasks.get(0);
    Assertions.assertThat(result1)
        .containsEntry("id", task3.getId().toString())
        .containsEntry("title", "title3")
        .containsEntry("description", null)
        .containsEntry("priority", "HIGH")
        .containsEntry("status", "COMPLETE")
        .containsEntry("createdAt", task3.getCreatedAt().toEpochMilli())
        .containsEntry("reminderAt", null)
        .containsEntry("reminderAllDay", null);

    Map<String, Object> result2 = findTasks.get(1);
    Assertions.assertThat(result2)
        .containsEntry("id", task2.getId().toString())
        .containsEntry("title", "title2")
        .containsEntry("description", null)
        .containsEntry("priority", "LOW")
        .containsEntry("status", "OPEN")
        .containsEntry("createdAt", task2.getCreatedAt().toEpochMilli())
        .containsEntry("reminderAt", null)
        .containsEntry("reminderAllDay", null);

    Map<String, Object> result3 = findTasks.get(2);
    Assertions.assertThat(result3)
        .containsEntry("id", task1.getId().toString())
        .containsEntry("title", "title1")
        .containsEntry("description", null)
        .containsEntry("priority", "MEDIUM")
        .containsEntry("status", "OPEN")
        .containsEntry("createdAt", task1.getCreatedAt().toEpochMilli())
        .containsEntry("reminderAt", null)
        .containsEntry("reminderAllDay", null);
  }

  @Test
  void givenALowPriorityTheFindTasksShouldReturnTheTasksOfTheRequesterWithLowPriority()
      throws Exception {
    // Given
    taskRepository.createTask(
        "00000000-0000-0000-0000-000000000000",
        "title1",
        null,
        Priority.MEDIUM,
        Status.OPEN,
        null,
        null);

    Task task1 =
        taskRepository.createTask(
            "00000000-0000-0000-0000-000000000000",
            "title2",
            null,
            Priority.LOW,
            Status.OPEN,
            null,
            null);

    Task task2 =
        taskRepository.createTask(
            "00000000-0000-0000-0000-000000000000",
            "title3",
            null,
            Priority.LOW,
            Status.COMPLETE,
            null,
            null);

    taskRepository.createTask(
        "11111111-1111-1111-1111-111111111111",
        "titleX",
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
            "query{findTasks(priority: LOW){"
                + "id title description priority status createdAt reminderAt reminderAllDay"
                + "}}"));

    // When
    Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
    List<Map<String, Object>> findTasks =
        TestUtils.jsonResponseToList(response.getContent(), "findTasks");

    Assertions.assertThat(findTasks).isNotNull().hasSize(2);

    Map<String, Object> result1 = findTasks.get(0);
    Assertions.assertThat(result1)
        .containsEntry("id", task2.getId().toString())
        .containsEntry("title", "title3")
        .containsEntry("description", null)
        .containsEntry("priority", "LOW")
        .containsEntry("status", "COMPLETE")
        .containsEntry("createdAt", task2.getCreatedAt().toEpochMilli())
        .containsEntry("reminderAt", null)
        .containsEntry("reminderAllDay", null);

    Map<String, Object> result2 = findTasks.get(1);
    Assertions.assertThat(result2)
        .containsEntry("id", task1.getId().toString())
        .containsEntry("title", "title2")
        .containsEntry("description", null)
        .containsEntry("priority", "LOW")
        .containsEntry("status", "OPEN")
        .containsEntry("createdAt", task1.getCreatedAt().toEpochMilli())
        .containsEntry("reminderAt", null)
        .containsEntry("reminderAllDay", null);
  }

  @Test
  void givenACompleteStatusAndAHighPriorityTheFindTasksShouldReturnTheMatchingTasksOfTheRequester()
      throws Exception {
    // Given
    taskRepository.createTask(
        "00000000-0000-0000-0000-000000000000",
        "title2",
        null,
        Priority.LOW,
        Status.OPEN,
        null,
        null);

    Task task =
        taskRepository.createTask(
            "00000000-0000-0000-0000-000000000000",
            "title3",
            null,
            Priority.HIGH,
            Status.COMPLETE,
            null,
            null);

    taskRepository.createTask(
        "11111111-1111-1111-1111-111111111111",
        "titleX",
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
            "query{findTasks(status: COMPLETE, priority: HIGH){"
                + "id title description priority status createdAt reminderAt reminderAllDay"
                + "}}"));

    // When
    Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
    List<Map<String, Object>> findTasks =
        TestUtils.jsonResponseToList(response.getContent(), "findTasks");

    Assertions.assertThat(findTasks).isNotNull().hasSize(1);

    Map<String, Object> result1 = findTasks.get(0);
    Assertions.assertThat(result1)
        .containsEntry("id", task.getId().toString())
        .containsEntry("title", "title3")
        .containsEntry("description", null)
        .containsEntry("priority", "HIGH")
        .containsEntry("status", "COMPLETE")
        .containsEntry("createdAt", task.getCreatedAt().toEpochMilli())
        .containsEntry("reminderAt", null)
        .containsEntry("reminderAllDay", null);
  }

  @Test
  void givenANonMatchingPriorityAndStatusInputTheFindTasksShouldReturnAnEmptyListOfTasks()
      throws Exception {
    // Given
    taskRepository.createTask(
        "00000000-0000-0000-0000-000000000000",
        "title2",
        null,
        Priority.LOW,
        Status.OPEN,
        null,
        null);

    taskRepository.createTask(
        "00000000-0000-0000-0000-000000000000",
        "title3",
        null,
        Priority.HIGH,
        Status.COMPLETE,
        null,
        null);

    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setHeader(HttpHeader.COOKIE.toString(), "ZM_AUTH_TOKEN=fake-user-cookie");
    request.setContent(
        TestUtils.queryPayload(
            "query{findTasks(status: COMPLETE, priority: MEDIUM){"
                + "id title description priority status createdAt reminderAt reminderAllDay"
                + "}}"));

    // When
    Response response =
        HttpTester.parseResponse(
            HttpTester.from(httpLocalConnector.getResponse(request.generate())));

    // Then
    Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
    List<Map<String, Object>> findTasks =
        TestUtils.jsonResponseToList(response.getContent(), "findTasks");

    Assertions.assertThat(findTasks).isNotNull().isEmpty();
  }
}
