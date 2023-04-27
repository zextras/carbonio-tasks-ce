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
import io.ebean.ExpressionList;
import io.ebean.OrderBy;
import io.ebean.Query;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class TaskRepositoryEbeanTest {

  private Database ebeanDatabaseMock;
  private TaskRepository taskRepository;
  private Clock fakeClock;

  @BeforeEach
  void setup() {
    ebeanDatabaseMock = Mockito.mock(Database.class, Mockito.RETURNS_DEEP_STUBS);
    DatabaseConnectionManager connectionManagerMock = Mockito.mock(DatabaseConnectionManager.class);
    Mockito.when(connectionManagerMock.getEbeanDatabase()).thenReturn(ebeanDatabaseMock);
    fakeClock = Mockito.mock(Clock.class);
    taskRepository = new TaskRepositoryEbean(connectionManagerMock, fakeClock);
  }

  @Test
  void givenAllTaskAttributesTheCreateTaskShouldReturnANewTask() {
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
    Assertions.assertThat(newTask.getDescription()).isPresent().contains("super description");
    Assertions.assertThat(newTask.getPriority()).isEqualTo(Priority.MEDIUM);
    Assertions.assertThat(newTask.getStatus()).isEqualTo(Status.OPEN);
    Assertions.assertThat(newTask.getCreatedAt()).isEqualTo(Instant.ofEpochSecond(1));
    Assertions.assertThat(newTask.getReminderAt()).isPresent().contains(Instant.ofEpochSecond(10));
    Assertions.assertThat(newTask.getReminderAllDay()).isPresent().contains(Boolean.TRUE);
  }

  @Test
  void givenOnlyMandatoryTaskAttributesTheCreateTaskShouldReturnANewTask() {
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
    Assertions.assertThat(newTask.getReminderAllDay()).isEmpty();
  }

  @Test
  void givenAUserAStatusAPriorityTheGetTasksShouldReturnAListOfTasks() {
    // Given
    Task taskMock1 = Mockito.mock(Task.class);
    Task taskMock2 = Mockito.mock(Task.class);
    ExpressionList<Task> partialQueryMock = Mockito.mock(ExpressionList.class);
    OrderBy<Task> orderByMock = Mockito.mock(OrderBy.class);
    Query<Task> finalQueryMock = Mockito.mock(Query.class);

    Mockito.when(
            ebeanDatabaseMock
                .find(Task.class)
                .where()
                .eq("user_id", "6d162bee-3186-1111-bf31-59746a41600e"))
        .thenReturn(partialQueryMock);

    Mockito.when(partialQueryMock.eq("priority", Priority.MEDIUM)).thenReturn(partialQueryMock);
    Mockito.when(partialQueryMock.eq("status", Status.OPEN)).thenReturn(partialQueryMock);
    Mockito.when(partialQueryMock.order()).thenReturn(orderByMock);
    Mockito.when(orderByMock.desc("created_at")).thenReturn(finalQueryMock);
    Mockito.when(finalQueryMock.findList()).thenReturn(Arrays.asList(taskMock1, taskMock2));

    // When
    List<Task> openTasks =
        taskRepository.getTasks(
            "6d162bee-3186-1111-bf31-59746a41600e", Priority.MEDIUM, Status.OPEN);

    // Then
    Assertions.assertThat(openTasks).hasSize(2);
  }

  @Test
  void givenAUserAStatusAPriorityTheGetTasksShouldReturnAnEmptyList() {
    // Given
    ExpressionList<Task> partialQueryMock = Mockito.mock(ExpressionList.class);
    OrderBy<Task> orderByMock = Mockito.mock(OrderBy.class);
    Query<Task> finalQueryMock = Mockito.mock(Query.class);

    Mockito.when(
            ebeanDatabaseMock
                .find(Task.class)
                .where()
                .eq("user_id", "6d162bee-3186-1111-bf31-59746a41600e"))
        .thenReturn(partialQueryMock);

    Mockito.when(partialQueryMock.eq("priority", Priority.MEDIUM)).thenReturn(partialQueryMock);
    Mockito.when(partialQueryMock.eq("status", Status.OPEN)).thenReturn(partialQueryMock);
    Mockito.when(partialQueryMock.order()).thenReturn(orderByMock);
    Mockito.when(orderByMock.desc("created_at")).thenReturn(finalQueryMock);
    Mockito.when(finalQueryMock.findList()).thenReturn(Collections.emptyList());

    // When
    List<Task> openTasks =
        taskRepository.getTasks(
            "6d162bee-3186-1111-bf31-59746a41600e", Priority.LOW, Status.COMPLETE);

    // Then
    Assertions.assertThat(openTasks).isEmpty();
  }

  @Test
  void givenAUserAStatusTheGetTasksShouldReturnAListOfTasks() {
    // Given
    Task taskMock = Mockito.mock(Task.class);
    ExpressionList<Task> partialQueryMock = Mockito.mock(ExpressionList.class);
    OrderBy<Task> orderByMock = Mockito.mock(OrderBy.class);
    Query<Task> finalQueryMock = Mockito.mock(Query.class);

    Mockito.when(
            ebeanDatabaseMock
                .find(Task.class)
                .where()
                .eq("user_id", "6d162bee-3186-1111-bf31-59746a41600e"))
        .thenReturn(partialQueryMock);

    Mockito.when(partialQueryMock.eq("status", Status.OPEN)).thenReturn(partialQueryMock);
    Mockito.when(partialQueryMock.order()).thenReturn(orderByMock);
    Mockito.when(orderByMock.desc("created_at")).thenReturn(finalQueryMock);
    Mockito.when(finalQueryMock.findList()).thenReturn(Collections.singletonList(taskMock));

    // When
    List<Task> openTasks =
        taskRepository.getTasks("6d162bee-3186-1111-bf31-59746a41600e", null, Status.COMPLETE);

    // Then
    Assertions.assertThat(openTasks).hasSize(1);
  }

  @Test
  void givenAUserAPriorityTheGetTasksShouldReturnAListOfTasks() {
    // Given
    Task taskMock = Mockito.mock(Task.class);
    ExpressionList<Task> partialQueryMock = Mockito.mock(ExpressionList.class);
    OrderBy<Task> orderByMock = Mockito.mock(OrderBy.class);
    Query<Task> finalQueryMock = Mockito.mock(Query.class);

    Mockito.when(
            ebeanDatabaseMock
                .find(Task.class)
                .where()
                .eq("user_id", "6d162bee-3186-1111-bf31-59746a41600e"))
        .thenReturn(partialQueryMock);

    Mockito.when(partialQueryMock.eq("status", Priority.HIGH)).thenReturn(partialQueryMock);
    Mockito.when(partialQueryMock.order()).thenReturn(orderByMock);
    Mockito.when(orderByMock.desc("created_at")).thenReturn(finalQueryMock);
    Mockito.when(finalQueryMock.findList()).thenReturn(Collections.singletonList(taskMock));

    // When
    List<Task> openTasks =
        taskRepository.getTasks("6d162bee-3186-1111-bf31-59746a41600e", Priority.HIGH, null);

    // Then
    Assertions.assertThat(openTasks).hasSize(1);
  }

  @Test
  void givenAUserTheGetTasksShouldReturnAListOfOpenTasks() {
    // Given
    Task taskMock = Mockito.mock(Task.class);
    ExpressionList<Task> partialQueryMock = Mockito.mock(ExpressionList.class);
    OrderBy<Task> orderByMock = Mockito.mock(OrderBy.class);
    Query<Task> finalQueryMock = Mockito.mock(Query.class);

    Mockito.when(
            ebeanDatabaseMock
                .find(Task.class)
                .where()
                .eq("user_id", "6d162bee-3186-1111-bf31-59746a41600e"))
        .thenReturn(partialQueryMock);

    Mockito.when(partialQueryMock.eq("status", Status.OPEN)).thenReturn(partialQueryMock);
    Mockito.when(partialQueryMock.order()).thenReturn(orderByMock);
    Mockito.when(orderByMock.desc("created_at")).thenReturn(finalQueryMock);
    Mockito.when(finalQueryMock.findList()).thenReturn(Collections.singletonList(taskMock));

    // When
    List<Task> openTasks =
        taskRepository.getTasks("6d162bee-3186-1111-bf31-59746a41600e", Priority.HIGH, null);

    // Then
    Assertions.assertThat(openTasks).hasSize(1);
  }

  @Test
  void givenATaskIdAndAUserIdTheGetTaskShouldReturnTheRequestedTask() {
    // Given
    Mockito.when(
            ebeanDatabaseMock
                .find(Task.class)
                .where()
                .idEq(UUID.fromString("6d162bee-3186-0000-bf31-59746a41600e"))
                .eq("user_id", "6d162bee-3186-1111-bf31-59746a41600e")
                .findOneOrEmpty())
        .thenReturn(Optional.of(Mockito.mock(Task.class)));

    // When
    Optional<Task> optTask =
        taskRepository.getTask(
            UUID.fromString("6d162bee-3186-0000-bf31-59746a41600e"),
            "6d162bee-3186-1111-bf31-59746a41600e");

    // Then
    Assertions.assertThat(optTask).isPresent();
  }

  @Test
  void givenANotExistingTaskIdTheGetTaskShouldReturnAnOptionalEmpty() {
    // Given
    Mockito.when(
            ebeanDatabaseMock
                .find(Task.class)
                .where()
                .idEq(UUID.fromString("6d162bee-3186-0000-bf31-59746a41600e"))
                .eq("user_id", "6d162bee-3186-1111-bf31-59746a41600e")
                .findOneOrEmpty())
        .thenReturn(Optional.of(Mockito.mock(Task.class)));

    // When
    Optional<Task> optTask =
        taskRepository.getTask(
            UUID.fromString("00000000-3186-0000-bf31-59746a41600e"),
            "6d162bee-3186-1111-bf31-59746a41600e");

    // Then
    Assertions.assertThat(optTask).isEmpty();
  }

  @Test
  void givenATaskIdAndADifferentUserIdTheGetTaskShouldReturnAnOptionalEmpty() {
    // Given
    Mockito.when(
            ebeanDatabaseMock
                .find(Task.class)
                .where()
                .idEq(UUID.fromString("6d162bee-3186-0000-bf31-59746a41600e"))
                .eq("user_id", "6d162bee-3186-1111-bf31-59746a41600e")
                .findOneOrEmpty())
        .thenReturn(Optional.of(Mockito.mock(Task.class)));

    // When
    Optional<Task> optTask =
        taskRepository.getTask(
            UUID.fromString("6d162bee-3186-0000-bf31-59746a41600e"), "wrong-user-id");

    // Then
    Assertions.assertThat(optTask).isEmpty();
  }

  @Test
  void givenAnUpdatedTaskTheUpdateTaskShouldSaveIt() {
    // Given
    Task taskMock = Mockito.mock(Task.class);

    // When
    taskRepository.updateTask(taskMock);

    // Then
    Mockito.verify(ebeanDatabaseMock, Mockito.times(1)).update(taskMock);
  }
}
