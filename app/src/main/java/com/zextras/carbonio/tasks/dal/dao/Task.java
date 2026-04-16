// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal.dao;

import com.zextras.carbonio.tasks.Constants.DatabaseSchema.Tables;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA/Panache entity representing a record in the {@code task} table.
 *
 * <p>This class uses the Panache active-record pattern via {@link PanacheEntityBase}. All CRUD
 * operations are delegated to {@link TaskRepository}.
 */
@Entity
@Table(name = Tables.TASK)
public class Task extends PanacheEntityBase {

  @Id
  @Column(name = Tables.Task.ID, nullable = false)
  public UUID id;

  @Column(name = Tables.Task.USER_ID, nullable = false, length = 36)
  public String userId;

  @Column(name = Tables.Task.TITLE, nullable = false, length = 1024)
  public String title;

  @Column(name = Tables.Task.DESCRIPTION, nullable = true, length = 4096)
  public String description;

  @Enumerated(EnumType.STRING)
  @Column(name = Tables.Task.PRIORITY, nullable = false)
  public Priority priority;

  @Enumerated(EnumType.STRING)
  @Column(name = Tables.Task.STATUS, nullable = false)
  public Status status;

  @Column(name = Tables.Task.CREATED_AT, nullable = false)
  public Instant createdAt;

  @Column(name = Tables.Task.REMINDER_AT, nullable = true)
  public Instant reminderAt;

  @Column(name = Tables.Task.REMINDER_ALL_DAY, nullable = true)
  public Boolean reminderAllDay;

  /** Required by JPA/Hibernate. */
  public Task() {}

  public Task(
      UUID id,
      String userId,
      String title,
      @Nullable String description,
      Priority priority,
      Status status,
      Instant createdAt,
      @Nullable Instant reminderAt,
      @Nullable Boolean reminderAllDay) {
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

  public void setTitle(String title) {
    this.title = title;
  }

  public Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Priority getPriority() {
    return priority;
  }

  public void setPriority(Priority priority) {
    this.priority = priority;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Optional<Instant> getReminderAt() {
    return Optional.ofNullable(reminderAt);
  }

  public void setReminderAt(Instant reminderAt) {
    this.reminderAt = reminderAt;
  }

  public Optional<Boolean> getReminderAllDay() {
    return Optional.ofNullable(reminderAllDay);
  }

  public void setReminderAllDay(Boolean reminderAllDay) {
    this.reminderAllDay = reminderAllDay;
  }
}
