// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal.repositories;

import com.zextras.carbonio.tasks.dal.dao.Priority;
import com.zextras.carbonio.tasks.dal.dao.Status;
import com.zextras.carbonio.tasks.dal.dao.Task;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link TaskRepository} creation logic. The Panache persistence calls are not
 * tested here (they need a real DB) — that is covered by integration tests.
 *
 * <p>This class only validates the factory method that assembles a {@link Task} from inputs.
 */
class TaskRepositoryTest {

  private Clock fakeClock;

  @BeforeEach
  void setUp() {
    fakeClock = Mockito.mock(Clock.class);
    Mockito.when(fakeClock.instant()).thenReturn(Instant.ofEpochSecond(1));
  }

  @Test
  void givenAllTaskAttributesTheCreateTaskShouldBuildANewTask() {
    // This test verifies the Task constructor and getter logic, decoupled from Panache.
    Task task =
        new Task(
            UUID.randomUUID(),
            "user-id",
            "fake-title",
            "super description",
            Priority.MEDIUM,
            Status.OPEN,
            fakeClock.instant(),
            Instant.ofEpochSecond(10),
            true);

    Assertions.assertThat(task.getId()).isNotNull().isInstanceOf(UUID.class);
    Assertions.assertThat(task.getUserId()).isEqualTo("user-id");
    Assertions.assertThat(task.getTitle()).isEqualTo("fake-title");
    Assertions.assertThat(task.getDescription()).isPresent().contains("super description");
    Assertions.assertThat(task.getPriority()).isEqualTo(Priority.MEDIUM);
    Assertions.assertThat(task.getStatus()).isEqualTo(Status.OPEN);
    Assertions.assertThat(task.getCreatedAt()).isEqualTo(Instant.ofEpochSecond(1));
    Assertions.assertThat(task.getReminderAt()).isPresent().contains(Instant.ofEpochSecond(10));
    Assertions.assertThat(task.getReminderAllDay()).isPresent().contains(Boolean.TRUE);
  }

  @Test
  void givenOnlyMandatoryTaskAttributesTheTaskShouldHaveNullOptionals() {
    Task task =
        new Task(
            UUID.randomUUID(),
            "user-id",
            "title",
            null,
            Priority.HIGH,
            Status.OPEN,
            fakeClock.instant(),
            null,
            null);

    Assertions.assertThat(task.getDescription()).isEmpty();
    Assertions.assertThat(task.getReminderAt()).isEmpty();
    Assertions.assertThat(task.getReminderAllDay()).isEmpty();
  }

  @Test
  void settersShouldMutateFieldsCorrectly() {
    Task task =
        new Task(
            UUID.randomUUID(),
            "user-id",
            "original-title",
            null,
            Priority.LOW,
            Status.OPEN,
            fakeClock.instant(),
            null,
            null);

    task.setTitle("updated-title");
    task.setDescription("new-description");
    task.setPriority(Priority.HIGH);
    task.setStatus(Status.COMPLETE);
    task.setReminderAt(Instant.ofEpochSecond(999));
    task.setReminderAllDay(true);

    Assertions.assertThat(task.getTitle()).isEqualTo("updated-title");
    Assertions.assertThat(task.getDescription()).isPresent().contains("new-description");
    Assertions.assertThat(task.getPriority()).isEqualTo(Priority.HIGH);
    Assertions.assertThat(task.getStatus()).isEqualTo(Status.COMPLETE);
    Assertions.assertThat(task.getReminderAt()).isPresent().contains(Instant.ofEpochSecond(999));
    Assertions.assertThat(task.getReminderAllDay()).isPresent().contains(Boolean.TRUE);
  }
}
