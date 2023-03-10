// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal.repositories.impl;

import com.zextras.carbonio.tasks.dal.DatabaseConnectionManager;
import com.zextras.carbonio.tasks.dal.dao.Priority;
import com.zextras.carbonio.tasks.dal.dao.Status;
import com.zextras.carbonio.tasks.dal.dao.Task;
import com.zextras.carbonio.tasks.dal.repositories.TaskRepository;
import io.ebean.Database;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class TaskRepositoryEbeanTest {

  private Database ebeanDatabaseMock;
  private TaskRepository taskRepository;
  private Clock fakeClock;

  @BeforeEach
  public void setup() {
    ebeanDatabaseMock = Mockito.mock(Database.class, Mockito.RETURNS_DEEP_STUBS);
    DatabaseConnectionManager connectionManagerMock = Mockito.mock(DatabaseConnectionManager.class);
    Mockito.when(connectionManagerMock.getEbeanDatabase()).thenReturn(ebeanDatabaseMock);
    fakeClock = Mockito.mock(Clock.class);
    taskRepository = new TaskRepositoryEbean(connectionManagerMock, fakeClock);
  }

  @Test
  public void givenAllTaskAttributesTheCreateTaskShouldReturnANewTask() {
    // Given & When
    Mockito.when(fakeClock.instant()).thenReturn(Instant.ofEpochSecond(1));

    Task newTask =
        taskRepository.createTask(
            "6d162bee-3186-1111-bf31-59746a41600e",
            "fake-title",
            "super description",
            Priority.MEDIUM,
            Status.OPEN,
            Instant.ofEpochSecond(10),
            true);

    // Then
    ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
    Mockito.verify(ebeanDatabaseMock, Mockito.times(1)).insert(taskCaptor.capture());

    Assertions.assertThat(taskCaptor.getValue()).isEqualTo(newTask);

    Assertions.assertThat(newTask.getId()).isNotNull().isInstanceOf(UUID.class);
    Assertions.assertThat(newTask.getUserId()).isEqualTo("6d162bee-3186-1111-bf31-59746a41600e");
    Assertions.assertThat(newTask.getTitle()).isEqualTo("fake-title");
    Assertions.assertThat(newTask.getDescription()).isPresent();
    Assertions.assertThat(newTask.getDescription().get()).isEqualTo("super description");
    Assertions.assertThat(newTask.getPriority()).isEqualTo(Priority.MEDIUM);
    Assertions.assertThat(newTask.getStatus()).isEqualTo(Status.OPEN);
    Assertions.assertThat(newTask.getCreatedAt()).isEqualTo(Instant.ofEpochSecond(1));
    Assertions.assertThat(newTask.getReminderAt()).isPresent();
    Assertions.assertThat(newTask.getReminderAt().get()).isEqualTo(Instant.ofEpochSecond(10));
    Assertions.assertThat(newTask.getReminderAllDay()).isEqualTo(Boolean.TRUE);
  }

  @Test
  public void givenOnlyMandatoryTaskAttributesTheCreateTaskShouldReturnANewTask() {
    // Given & When
    Mockito.when(fakeClock.instant()).thenReturn(Instant.ofEpochSecond(1));

    Task newTask =
        taskRepository.createTask(
            "6d162bee-3186-1111-bf31-59746a41600e",
            "fake-title",
            null,
            Priority.MEDIUM,
            Status.OPEN,
            null,
            null);

    // Then
    ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
    Mockito.verify(ebeanDatabaseMock, Mockito.times(1)).insert(taskCaptor.capture());

    Assertions.assertThat(taskCaptor.getValue()).isEqualTo(newTask);

    Assertions.assertThat(newTask.getId()).isNotNull().isInstanceOf(UUID.class);
    Assertions.assertThat(newTask.getUserId()).isEqualTo("6d162bee-3186-1111-bf31-59746a41600e");
    Assertions.assertThat(newTask.getTitle()).isEqualTo("fake-title");
    Assertions.assertThat(newTask.getDescription()).isEmpty();
    Assertions.assertThat(newTask.getPriority()).isEqualTo(Priority.MEDIUM);
    Assertions.assertThat(newTask.getStatus()).isEqualTo(Status.OPEN);
    Assertions.assertThat(newTask.getCreatedAt()).isEqualTo(Instant.ofEpochSecond(1));
    Assertions.assertThat(newTask.getReminderAt()).isEmpty();
    Assertions.assertThat(newTask.getReminderAllDay()).isEqualTo(Boolean.FALSE);
  }

  @Test
  public void givenAListOfOpenTasksTheGetOpenTasksShouldReturnThemAll() {
    // Given
    Task taskMock1 = Mockito.mock(Task.class);
    Task taskMock2 = Mockito.mock(Task.class);

    Mockito.when(
            ebeanDatabaseMock
                .find(Task.class)
                .where()
                .eq("user_id", "6d162bee-3186-1111-bf31-59746a41600e")
                .eq("status", Status.OPEN)
                .findList())
        .thenReturn(Arrays.asList(taskMock1, taskMock2));

    // When
    List<Task> openTasks = taskRepository.getOpenTasks("6d162bee-3186-1111-bf31-59746a41600e");

    // Then
    Assertions.assertThat(openTasks.size()).isEqualTo(2);
  }

  @Test
  public void withoutOpenTasksTheGetOpenTaskShouldReturnAnEmptyList() {
    // Given
    Mockito.when(
            ebeanDatabaseMock
                .find(Task.class)
                .where()
                .eq("user_id", "6d162bee-3186-1111-bf31-59746a41600e")
                .eq("status", Status.OPEN)
                .findList())
        .thenReturn(Collections.emptyList());

    // When
    List<Task> openTasks = taskRepository.getOpenTasks("6d162bee-3186-1111-bf31-59746a41600e");

    // Then
    Assertions.assertThat(openTasks.size()).isEqualTo(0);
  }
}
