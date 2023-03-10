// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal.repositories.impl;

import com.google.inject.Inject;
import com.zextras.carbonio.tasks.Constants.Database.Tables;
import com.zextras.carbonio.tasks.dal.DatabaseConnectionManager;
import com.zextras.carbonio.tasks.dal.dao.Priority;
import com.zextras.carbonio.tasks.dal.dao.Status;
import com.zextras.carbonio.tasks.dal.dao.Task;
import com.zextras.carbonio.tasks.dal.repositories.TaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

public class TaskRepositoryEbean implements TaskRepository {

  private final DatabaseConnectionManager dbConnectionManager;
  private final Clock clock;

  @Inject
  public TaskRepositoryEbean(DatabaseConnectionManager dbConnectionManager, Clock clock) {
    this.dbConnectionManager = dbConnectionManager;
    this.clock = clock;
  }

  @Override
  public Task createTask(
      String userId,
      String title,
      @Nullable String description,
      Priority priority,
      Status status,
      @Nullable Instant reminderAt,
      @Nullable Boolean reminderAllDay) {

    Task newTask =
        new Task(
            UUID.randomUUID(),
            userId,
            title,
            description,
            priority,
            status,
            clock.instant(),
            reminderAt,
            reminderAllDay != null ? reminderAllDay : false);

    dbConnectionManager.getEbeanDatabase().insert(newTask);
    return newTask;
  }

  @Override
  public List<Task> getOpenTasks(String userId) {
    return dbConnectionManager
        .getEbeanDatabase()
        .find(Task.class)
        .where()
        .eq(Tables.Task.USER_ID, userId)
        .eq(Tables.Task.STATUS, Status.OPEN)
        .findList();
  }
}
