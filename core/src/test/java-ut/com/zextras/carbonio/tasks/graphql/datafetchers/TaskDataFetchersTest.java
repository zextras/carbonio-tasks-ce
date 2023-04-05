// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql.datafetchers;

import com.google.common.collect.ImmutableMap;
import com.zextras.carbonio.tasks.dal.dao.Priority;
import com.zextras.carbonio.tasks.dal.dao.Status;
import com.zextras.carbonio.tasks.dal.dao.Task;
import com.zextras.carbonio.tasks.dal.repositories.TaskRepository;
import graphql.GraphQLContext;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TaskDataFetchersTest {

  private TaskRepository taskRepositoryMock;
  private TaskDataFetchers taskDataFetchers;

  @BeforeEach
  void setUp() {
    taskRepositoryMock = Mockito.mock(TaskRepository.class);
    taskDataFetchers = new TaskDataFetchers(taskRepositoryMock);
  }

  @Test
  void givenAnExistingTaskIdTheGetTaskDataFetcherShouldReturnAResultWithTheTask() throws Exception {
    // Given
    Task requestedTaskMock = Mockito.mock(Task.class);
    Mockito.when(requestedTaskMock.getId())
        .thenReturn(UUID.fromString("6d162bee-3186-0000-bf31-59746a41600e"));
    Mockito.when(requestedTaskMock.getTitle()).thenReturn("title1");
    Mockito.when(requestedTaskMock.getPriority()).thenReturn(Priority.MEDIUM);
    Mockito.when(requestedTaskMock.getStatus()).thenReturn(Status.COMPLETE);
    Mockito.when(requestedTaskMock.getCreatedAt()).thenReturn(Instant.ofEpochMilli(10));

    Mockito.when(
            taskRepositoryMock.getTask(
                UUID.fromString("6d162bee-3186-0000-bf31-59746a41600e"),
                "00000000-0000-0000-0000-000000000000"))
        .thenReturn(Optional.of(requestedTaskMock));

    GraphQLContext graphQLContextMock = Mockito.mock(GraphQLContext.class);
    Mockito.when(graphQLContextMock.get("requesterId"))
        .thenReturn("00000000-0000-0000-0000-000000000000");

    DataFetchingEnvironment environmentMock = Mockito.mock(DataFetchingEnvironment.class);
    Mockito.when(environmentMock.getArgument("taskId"))
        .thenReturn("6d162bee-3186-0000-bf31-59746a41600e");
    Mockito.when(environmentMock.getGraphQlContext()).thenReturn(graphQLContextMock);

    // When
    DataFetcherResult<Map<String, Object>> dataFetcherResult =
        taskDataFetchers.getTask().get(environmentMock).get();

    // Then
    Assertions.assertThat(dataFetcherResult.getErrors()).isEmpty();
    Assertions.assertThat(dataFetcherResult.getData()).isNotNull();

    Map<String, Object> mapRequestedTask = dataFetcherResult.getData();

    Assertions.assertThat(mapRequestedTask)
        .containsEntry("id", UUID.fromString("6d162bee-3186-0000-bf31-59746a41600e"))
        .containsEntry("title", "title1")
        .doesNotContainKey("description")
        .containsEntry("priority", Priority.MEDIUM)
        .containsEntry("status", Status.COMPLETE)
        .containsEntry("createdAt", 10L)
        .doesNotContainKey("reminderAt")
        .doesNotContainKey("reminderAllDay");
  }

  @Test
  void givenANonExistingTaskIdTheGetTaskDataFetcherShouldReturnAnEmptyResultWithAnError()
      throws Exception {
    // Given
    Mockito.when(
            taskRepositoryMock.getTask(
                UUID.fromString("6d162bee-3186-0000-bf31-59746a41600e"),
                "00000000-0000-0000-0000-000000000000"))
        .thenReturn(Optional.empty());

    GraphQLContext graphQLContextMock = Mockito.mock(GraphQLContext.class);
    Mockito.when(graphQLContextMock.get("requesterId"))
        .thenReturn("00000000-0000-0000-0000-000000000000");

    DataFetchingEnvironment environmentMock = Mockito.mock(DataFetchingEnvironment.class);
    Mockito.when(environmentMock.getArgument("taskId"))
        .thenReturn("6d162bee-3186-0000-bf31-59746a41600e");
    Mockito.when(environmentMock.getGraphQlContext()).thenReturn(graphQLContextMock);

    // When
    DataFetcherResult<Map<String, Object>> dataFetcherResult =
        taskDataFetchers.getTask().get(environmentMock).get();

    // Then
    Assertions.assertThat(dataFetcherResult.getData()).isNull();
    Assertions.assertThat(dataFetcherResult.getErrors()).hasSize(1);

    Assertions.assertThat(dataFetcherResult.getErrors().get(0).getMessage())
        .isEqualTo("Could not find task with id 6d162bee-3186-0000-bf31-59746a41600e");
  }

  @Test
  void
      givenAnExistingTaskAndAllUpdatedFieldsTheUpdateTaskDataFetcherShouldReturnAResultWithTheUpdatedTask()
          throws Exception {
    // Given
    GraphQLContext graphQLContextMock = Mockito.mock(GraphQLContext.class);
    Mockito.when(graphQLContextMock.get("requesterId"))
        .thenReturn("00000000-0000-0000-0000-000000000000");

    // Task attributes to update
    Map<String, Object> updateTask =
        ImmutableMap.<String, Object>builder()
            .put("id", "11111111-1111-1111-1111-111111111111")
            .put("title", "New title")
            .put("description", "New description")
            .put("priority", Priority.LOW)
            .put("status", Status.COMPLETE)
            .put("reminderAt", 55L)
            .put("reminderAllDay", true)
            .build();

    DataFetchingEnvironment environmentMock = Mockito.mock(DataFetchingEnvironment.class);
    Mockito.when(environmentMock.getGraphQlContext()).thenReturn(graphQLContextMock);
    Mockito.when(environmentMock.getArgument("updateTask")).thenReturn(updateTask);

    Task existingTaskMock =
        new Task(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "00000000-0000-0000-0000-000000000000",
            "title",
            null,
            Priority.HIGH,
            Status.OPEN,
            Instant.ofEpochMilli(5L),
            null,
            false);

    Mockito.when(
            taskRepositoryMock.getTask(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "00000000-0000-0000-0000-000000000000"))
        .thenReturn(Optional.of(existingTaskMock));

    // When
    DataFetcherResult<Map<String, Object>> dataFetcherResult =
        taskDataFetchers.updateTask().get(environmentMock).get();

    // Then
    Assertions.assertThat(dataFetcherResult.getErrors()).isEmpty();
    Assertions.assertThat(dataFetcherResult.getData()).isNotNull();

    Map<String, Object> mapUpdatedTask = dataFetcherResult.getData();
    Assertions.assertThat(mapUpdatedTask)
        .containsEntry("id", UUID.fromString("11111111-1111-1111-1111-111111111111"))
        .containsEntry("title", "New title")
        .containsEntry("description", "New description")
        .containsEntry("priority", Priority.LOW)
        .containsEntry("status", Status.COMPLETE)
        .containsEntry("createdAt", 5L)
        .containsEntry("reminderAt", 55L)
        .containsEntry("reminderAllDay", Boolean.TRUE);
  }

  @Test
  void
      givenAnExistingTaskAndNoFieldsToUpdateTheUpdateTaskDataFetcherShouldReturnAResultWithTheUntouchedTask()
          throws Exception {
    // Given
    GraphQLContext graphQLContextMock = Mockito.mock(GraphQLContext.class);
    Mockito.when(graphQLContextMock.get("requesterId"))
        .thenReturn("00000000-0000-0000-0000-000000000000");

    // Task attributes to update
    Map<String, Object> updateTask =
        ImmutableMap.<String, Object>builder()
            .put("id", "11111111-1111-1111-1111-111111111111")
            .build();

    DataFetchingEnvironment environmentMock = Mockito.mock(DataFetchingEnvironment.class);
    Mockito.when(environmentMock.getGraphQlContext()).thenReturn(graphQLContextMock);
    Mockito.when(environmentMock.getArgument("updateTask")).thenReturn(updateTask);

    Task existingTaskMock =
        new Task(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "00000000-0000-0000-0000-000000000000",
            "title",
            "description",
            Priority.HIGH,
            Status.OPEN,
            Instant.ofEpochMilli(5L),
            null,
            false);

    Mockito.when(
            taskRepositoryMock.getTask(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "00000000-0000-0000-0000-000000000000"))
        .thenReturn(Optional.of(existingTaskMock));

    // When
    DataFetcherResult<Map<String, Object>> dataFetcherResult =
        taskDataFetchers.updateTask().get(environmentMock).get();

    // Then
    Assertions.assertThat(dataFetcherResult.getErrors()).isEmpty();
    Assertions.assertThat(dataFetcherResult.getData()).isNotNull();

    Map<String, Object> mapUpdatedTask = dataFetcherResult.getData();
    Assertions.assertThat(mapUpdatedTask)
        .containsEntry("id", UUID.fromString("11111111-1111-1111-1111-111111111111"))
        .containsEntry("title", "title")
        .containsEntry("description", "description")
        .containsEntry("priority", Priority.HIGH)
        .containsEntry("status", Status.OPEN)
        .containsEntry("createdAt", 5L)
        .doesNotContainKey("reminderAt")
        .doesNotContainKey("reminderAllDay");
  }

  @Test
  void
      givenAnExistingTaskAndReminderAtToZeroTheUpdateTaskDataFetcherShouldReturnAResultWithTheTaskWithTheReminderReset()
          throws Exception {
    // Given
    GraphQLContext graphQLContextMock = Mockito.mock(GraphQLContext.class);
    Mockito.when(graphQLContextMock.get("requesterId"))
        .thenReturn("00000000-0000-0000-0000-000000000000");

    // Task attributes to update
    Map<String, Object> updateTask =
        ImmutableMap.<String, Object>builder()
            .put("id", "11111111-1111-1111-1111-111111111111")
            .put("reminderAt", 0L)
            .put("reminderAllDay", false)
            .build();

    DataFetchingEnvironment environmentMock = Mockito.mock(DataFetchingEnvironment.class);
    Mockito.when(environmentMock.getGraphQlContext()).thenReturn(graphQLContextMock);
    Mockito.when(environmentMock.getArgument("updateTask")).thenReturn(updateTask);

    Task existingTaskMock =
        new Task(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "00000000-0000-0000-0000-000000000000",
            "title",
            "description",
            Priority.HIGH,
            Status.OPEN,
            Instant.ofEpochMilli(5L),
            Instant.ofEpochMilli(55L),
            true);

    Mockito.when(
            taskRepositoryMock.getTask(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "00000000-0000-0000-0000-000000000000"))
        .thenReturn(Optional.of(existingTaskMock));

    // When
    DataFetcherResult<Map<String, Object>> dataFetcherResult =
        taskDataFetchers.updateTask().get(environmentMock).get();

    // Then
    Assertions.assertThat(dataFetcherResult.getErrors()).isEmpty();
    Assertions.assertThat(dataFetcherResult.getData()).isNotNull();

    Map<String, Object> mapUpdatedTask = dataFetcherResult.getData();
    Assertions.assertThat(mapUpdatedTask)
        .containsEntry("id", UUID.fromString("11111111-1111-1111-1111-111111111111"))
        .doesNotContainKey("reminderAt")
        .doesNotContainKey("reminderAllDay");
  }

  @Test
  void givenANotExistingTaskTheUpdateTaskDataFetcherShouldReturnAnEmptyResultWithAnError()
      throws Exception {
    // Given
    GraphQLContext graphQLContextMock = Mockito.mock(GraphQLContext.class);
    Mockito.when(graphQLContextMock.get("requesterId"))
        .thenReturn("00000000-0000-0000-0000-000000000000");

    // Task attributes to update
    Map<String, Object> updateTask =
        ImmutableMap.<String, Object>builder()
            .put("id", "11111111-1111-1111-1111-111111111111")
            .put("title", "New title")
            .build();

    DataFetchingEnvironment environmentMock = Mockito.mock(DataFetchingEnvironment.class);
    Mockito.when(environmentMock.getGraphQlContext()).thenReturn(graphQLContextMock);
    Mockito.when(environmentMock.getArgument("updateTask")).thenReturn(updateTask);

    Mockito.when(
            taskRepositoryMock.getTask(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "00000000-0000-0000-0000-000000000000"))
        .thenReturn(Optional.empty());

    // When
    DataFetcherResult<Map<String, Object>> dataFetcherResult =
        taskDataFetchers.updateTask().get(environmentMock).get();

    // Then
    Assertions.assertThat(dataFetcherResult.getData()).isNull();
    Assertions.assertThat(dataFetcherResult.getErrors()).hasSize(1);

    Assertions.assertThat(dataFetcherResult.getErrors().get(0).getMessage())
        .isEqualTo("Could not find task with id 11111111-1111-1111-1111-111111111111");
  }
}
