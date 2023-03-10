// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal.dao;

import com.zextras.carbonio.tasks.Constants.Database;
import com.zextras.carbonio.tasks.Constants.Database.Tables;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Represents an Ebean {@link Task} entity that matches a record of the {@link Tables#TASK} table.
 */
@Entity
@Table(name = Database.Tables.TASK)
public class Task {

  @Id
  @Column(name = Tables.Task.ID, nullable = false)
  private UUID id;

  @Column(name = Tables.Task.USER_ID, nullable = false, length = 36)
  private String userId;

  @Column(name = Tables.Task.TITLE, nullable = false, length = 1024)
  private String title;

  @Column(name = Tables.Task.DESCRIPTION, nullable = true, length = 4096)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = Tables.Task.PRIORITY, nullable = false)
  private Priority priority;

  @Enumerated(EnumType.STRING)
  @Column(name = Tables.Task.STATUS, nullable = false)
  private Status status;

  @Column(name = Tables.Task.CREATED_AT, nullable = false)
  private Instant createdAt;

  @Column(name = Tables.Task.REMINDER_AT, nullable = true)
  private Instant reminderAt;

  @Column(name = Tables.Task.REMINDER_ALL_DAY, nullable = false)
  private Boolean reminderAllDay;

  public Task(
      UUID id,
      String userId,
      String title,
      @Nullable String description,
      Priority priority,
      Status status,
      Instant createdAt,
      @Nullable Instant reminderAt,
      boolean reminderAllDay) {
    this.id = id;
    this.userId = userId;
    this.title = title;
    this.description = description;
    this.priority = priority;
    this.status = status;
    this.createdAt = createdAt;
    this.reminderAt = reminderAt;
    this.reminderAllDay = reminderAllDay;
  }

  public UUID getId() {
    return id;
  }

  public String getUserId() {
    return userId;
  }

  public String getTitle() {
    return title;
  }

  public Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }

  public Priority getPriority() {
    return priority;
  }

  public Status getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Optional<Instant> getReminderAt() {
    return Optional.ofNullable(reminderAt);
  }

  public boolean getReminderAllDay() {
    return reminderAllDay;
  }
}
