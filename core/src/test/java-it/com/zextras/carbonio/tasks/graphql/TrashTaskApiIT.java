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
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.server.LocalConnector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

public class TrashTaskApiIT {

  static Simulator simulator;
  static LocalConnector httpLocalConnector;
  static TaskRepository taskRepository;
  static String REQUESTER_COOKIE = "ZM_AUTH_TOKEN=fake-user-cookie";
  static String REQUESTER_ID = "0c07cb53-f942-4644-8166-fb1d3e41faf3";

  @BeforeAll
  static void init() {
    simulator =
        SimulatorBuilder.aSimulator()
            .init()
            .withDatabase()
            .withServiceDiscover()
            .withUserManagement(ImmutableMap.of("fake-user-cookie", REQUESTER_ID))
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
  void
      givenAnExistingTaskIdTheTrashTaskApiShouldMarkItAsDeletedAndReturn200WithTheIdOfTheTrashedTask()
          throws Exception {
    // Given
    Task taskToTrash =
        taskRepository.createTask(
            REQUESTER_ID, "title1", "description", Priority.HIGH, Status.OPEN, null, null);

    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setHeader(HttpHeader.COOKIE.toString(), REQUESTER_COOKIE);
    request.setContent(
        TestUtils.queryPayload(
            "mutation { trashTask(taskId: \\\"" + taskToTrash.getId().toString() + "\\\") }"));

    // When
    HttpTester.Response response =
        HttpTester.parseResponse(httpLocalConnector.getResponse(request.generate()));

    // Then
    Assertions.assertThat(response.getStatus()).isEqualTo(200);

    Optional<String> optStringResponse =
        TestUtils.jsonResponseToString(response.getContent(), "trashTask");
    Assertions.assertThat(optStringResponse).isPresent().contains(taskToTrash.getId().toString());
  }

  @Test
  void givenANotExistingTaskIdTheTrashTaskApiShouldReturn200WithAnErrorMessage() throws Exception {
    // Given
    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setHeader(HttpHeader.COOKIE.toString(), REQUESTER_COOKIE);
    request.setContent(
        TestUtils.queryPayload(
            "mutation { trashTask(taskId: \\\"1e39756c-bd40-4381-8415-af4244d7a3e8\\\") }"));

    // When
    HttpTester.Response response =
        HttpTester.parseResponse(httpLocalConnector.getResponse(request.generate()));

    // Then
    Assertions.assertThat(response.getStatus()).isEqualTo(200);

    List<String> errors = TestUtils.jsonResponseToErrors(response.getContent());
    Assertions.assertThat(errors)
        .hasSize(1)
        .contains("Could not find task with id 1e39756c-bd40-4381-8415-af4244d7a3e8");
  }

  @Test
  void givenAnExistingTaskIdOfAnotherUserTheTrashTaskApiShouldReturn200WithAErrorMessage()
      throws Exception {
    // Given

    Task taskToTrash =
        taskRepository.createTask(
            "00000000-0000-0000-0000-000000000000",
            "title1",
            "description",
            Priority.HIGH,
            Status.OPEN,
            null,
            null);

    HttpTester.Request request = HttpTester.newRequest();
    request.setMethod(HttpMethod.POST.toString());
    request.setURI("/graphql/");
    request.setHeader(HttpHeader.HOST.toString(), "test");
    request.setHeader(HttpHeader.COOKIE.toString(), REQUESTER_COOKIE);
    request.setContent(
        TestUtils.queryPayload(
            "mutation { trashTask(taskId: \\\"" + taskToTrash.getId().toString() + "\\\") }"));

    // When
    HttpTester.Response response =
        HttpTester.parseResponse(httpLocalConnector.getResponse(request.generate()));

    // Then
    Assertions.assertThat(response.getStatus()).isEqualTo(200);

    List<String> errors = TestUtils.jsonResponseToErrors(response.getContent());
    Assertions.assertThat(errors)
        .hasSize(1)
        .contains(String.format("Could not find task with id %s", taskToTrash.getId().toString()));
  }
}
