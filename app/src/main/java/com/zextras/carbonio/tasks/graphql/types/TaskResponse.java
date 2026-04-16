// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql.types;

import com.zextras.carbonio.tasks.dal.dao.Priority;
import com.zextras.carbonio.tasks.dal.dao.Status;
import org.eclipse.microprofile.graphql.Id;

/**
 * GraphQL output type for a {@link com.zextras.carbonio.tasks.dal.dao.Task}.
 *
 * <p>Dates are represented as epoch milliseconds (Long) — SmallRye maps these to BigInteger.
 */
public class TaskResponse {

  @Id
  private String id;
  private String title;
  private String description;
  private Priority priority;
  private Status status;
  private Long createdAt;
  private Long reminderAt;
  private Boolean reminderAllDay;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
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

  public Long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
  }

  public Long getReminderAt() {
    return reminderAt;
  }

  public void setReminderAt(Long reminderAt) {
    this.reminderAt = reminderAt;
  }

  public Boolean getReminderAllDay() {
    return reminderAllDay;
  }

  public void setReminderAllDay(Boolean reminderAllDay) {
    this.reminderAllDay = reminderAllDay;
  }
}
