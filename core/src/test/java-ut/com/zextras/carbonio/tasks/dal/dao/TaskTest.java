// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal.dao;

import java.time.Instant;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class TaskTest {

  @Test
  public void givenAllTaskAttributesTheTaskConstructorShouldCreateTaskObjectCorrectly() {
    // Given & When
    Task task =
        new Task(
            UUID.fromString("6d162bee-3186-1111-bf31-59746a41600e"),
            "6d162bee-3186-0000-bf31-59746a41600e",
            "fake-title",
            "fake-description",
            Priority.HIGH,
            Status.OPEN,
            Instant.ofEpochMilli(10),
            Instant.ofEpochMilli(15),
            false);

    // Then
    Assertions.assertThat(task.getId())
        .isEqualTo(UUID.fromString("6d162bee-3186-1111-bf31-59746a41600e"));
    Assertions.assertThat(task.getUserId()).isEqualTo("6d162bee-3186-0000-bf31-59746a41600e");
    Assertions.assertThat(task.getTitle()).isEqualTo("fake-title");
    Assertions.assertThat(task.getDescription()).isPresent();
    Assertions.assertThat(task.getDescription().get()).isEqualTo("fake-description");
    Assertions.assertThat(task.getPriority()).isEqualTo(Priority.HIGH);
    Assertions.assertThat(task.getStatus()).isEqualTo(Status.OPEN);
    Assertions.assertThat(task.getCreatedAt()).isEqualTo(Instant.ofEpochMilli(10));
    Assertions.assertThat(task.getReminderAt()).isPresent();
    Assertions.assertThat(task.getReminderAt().get()).isEqualTo(Instant.ofEpochMilli(15));
    Assertions.assertThat(task.getReminderAllDay()).isEqualTo(Boolean.FALSE);
  }

  @Test
  public void givenOnlyMandatoryTaskAttributesTheTaskConstructorShouldCreateTaskObjectCorrectly() {
    // Given & When
    Task task =
        new Task(
            UUID.fromString("6d162bee-3186-1111-bf31-59746a41600e"),
            "6d162bee-3186-0000-bf31-59746a41600e",
            "fake-title",
            null,
            Priority.LOW,
            Status.COMPLETE,
            Instant.ofEpochMilli(0),
            null,
            false);

    // Then
    Assertions.assertThat(task.getId())
        .isEqualTo(UUID.fromString("6d162bee-3186-1111-bf31-59746a41600e"));
    Assertions.assertThat(task.getUserId()).isEqualTo("6d162bee-3186-0000-bf31-59746a41600e");
    Assertions.assertThat(task.getTitle()).isEqualTo("fake-title");
    Assertions.assertThat(task.getDescription()).isEmpty();
    Assertions.assertThat(task.getPriority()).isEqualTo(Priority.LOW);
    Assertions.assertThat(task.getStatus()).isEqualTo(Status.COMPLETE);
    Assertions.assertThat(task.getCreatedAt()).isEqualTo(Instant.ofEpochMilli(0));
    Assertions.assertThat(task.getReminderAt()).isEmpty();
    Assertions.assertThat(task.getReminderAllDay()).isEqualTo(Boolean.FALSE);
  }
}
