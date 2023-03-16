// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql.datafetchers;

import com.zextras.carbonio.tasks.dal.dao.Priority;
import com.zextras.carbonio.tasks.dal.dao.Status;
import com.zextras.carbonio.tasks.dal.dao.Task;
import com.zextras.carbonio.tasks.dal.repositories.TaskRepository;
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

public class TaskDataFetchersTest {

  private TaskRepository taskRepositoryMock;
  private TaskDataFetchers taskDataFetchers;

  @BeforeEach
  public void setUp() {
    taskRepositoryMock = Mockito.mock(TaskRepository.class);
    taskDataFetchers = new TaskDataFetchers(taskRepositoryMock);
  }

  @Test
  public void givenAnExistingTaskIdTheGetTaskDataFetcherShouldReturnAResultWithTheTask()
      throws Exception {
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

    DataFetchingEnvironment environmentMock = Mockito.mock(DataFetchingEnvironment.class);
    Mockito.when(environmentMock.getArgument("taskId"))
        .thenReturn("6d162bee-3186-0000-bf31-59746a41600e");

    // When
    DataFetcherResult<Map<String, Object>> dataFetcherResult =
        taskDataFetchers.getTask().get(environmentMock).get();

    // Then
    Assertions.assertThat(dataFetcherResult.getErrors().size()).isEqualTo(0);
    Assertions.assertThat(dataFetcherResult.getData()).isNotNull();

    Map<String, Object> mapRequestedTask = dataFetcherResult.getData();
    Assertions.assertThat(mapRequestedTask.get("id"))
        .isEqualTo(UUID.fromString("6d162bee-3186-0000-bf31-59746a41600e"));
    Assertions.assertThat(mapRequestedTask.get("title")).isEqualTo("title1");
    Assertions.assertThat(mapRequestedTask.get("description")).isNull();
    Assertions.assertThat(mapRequestedTask.get("priority")).isEqualTo(Priority.MEDIUM);
    Assertions.assertThat(mapRequestedTask.get("status")).isEqualTo(Status.COMPLETE);
    Assertions.assertThat(mapRequestedTask.get("createdAt")).isEqualTo(10L);
    Assertions.assertThat(mapRequestedTask.get("reminderAt")).isNull();
    Assertions.assertThat(mapRequestedTask.get("reminderAllDay")).isNull();
  }

  @Test
  public void givenANonExistingTaskIdTheGetTaskDataFetcherShouldReturnAnEmptyResultWithAnError()
      throws Exception {
    // Given
    Mockito.when(
            taskRepositoryMock.getTask(
                UUID.fromString("6d162bee-3186-0000-bf31-59746a41600e"),
                "00000000-0000-0000-0000-000000000000"))
        .thenReturn(Optional.empty());

    DataFetchingEnvironment environmentMock = Mockito.mock(DataFetchingEnvironment.class);
    Mockito.when(environmentMock.getArgument("taskId"))
        .thenReturn("6d162bee-3186-0000-bf31-59746a41600e");

    // When
    DataFetcherResult<Map<String, Object>> dataFetcherResult =
        taskDataFetchers.getTask().get(environmentMock).get();

    // Then
    Assertions.assertThat(dataFetcherResult.getData()).isNull();
    Assertions.assertThat(dataFetcherResult.getErrors().size()).isEqualTo(1);

    Assertions.assertThat(dataFetcherResult.getErrors().get(0).getMessage())
        .isEqualTo("Could not find task with id 6d162bee-3186-0000-bf31-59746a41600e");
  }
}
