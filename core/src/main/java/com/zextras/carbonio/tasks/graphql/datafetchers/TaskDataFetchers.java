// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql.datafetchers;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.zextras.carbonio.tasks.Constants.GraphQL;
import com.zextras.carbonio.tasks.Constants.GraphQL.Inputs;
import com.zextras.carbonio.tasks.dal.dao.Priority;
import com.zextras.carbonio.tasks.dal.dao.Status;
import com.zextras.carbonio.tasks.dal.dao.Task;
import com.zextras.carbonio.tasks.dal.repositories.TaskRepository;
import graphql.schema.DataFetcher;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TaskDataFetchers {

  private final TaskRepository taskRepository;

  @Inject
  public TaskDataFetchers(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  public DataFetcher<CompletableFuture<List<Map<String, Object>>>> findTasks() {
    return environment ->
        CompletableFuture.supplyAsync(
            () -> {
              String userId = "00000000-0000-0000-0000-000000000000";
              Priority priority = environment.getArgument(Inputs.PRIORITY);
              Status status = environment.getArgument(Inputs.STATUS);

              return taskRepository.getTasks(userId, priority, status).stream()
                  .map(this::convertTaskToMap)
                  .collect(Collectors.toList());
            });
  }

  private Map<String, Object> convertTaskToMap(Task task) {
    ImmutableMap.Builder<String, Object> taskMapBuilder =
        ImmutableMap.<String, Object>builder()
            .put(GraphQL.Task.ID, task.getId())
            .put(GraphQL.Task.TITLE, task.getTitle())
            .put(GraphQL.Task.PRIORITY, task.getPriority())
            .put(GraphQL.Task.STATUS, task.getStatus())
            .put(GraphQL.Task.CREATED_AT, task.getCreatedAt().toEpochMilli());

    task.getDescription()
        .ifPresent(description -> taskMapBuilder.put(GraphQL.Task.DESCRIPTION, description));

    task.getReminderAt()
        .ifPresent(
            reminderAt -> {
              taskMapBuilder.put(GraphQL.Task.REMINDER_AT, reminderAt);
              taskMapBuilder.put(GraphQL.Task.REMINDER_ALL_DAY, task.getReminderAllDay());
            });

    return taskMapBuilder.build();
  }
}