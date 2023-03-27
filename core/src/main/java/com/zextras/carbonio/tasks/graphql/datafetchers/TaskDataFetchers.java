// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql.datafetchers;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.zextras.carbonio.tasks.Constants.GraphQL;
import com.zextras.carbonio.tasks.Constants.GraphQL.Context;
import com.zextras.carbonio.tasks.Constants.GraphQL.Inputs;
import com.zextras.carbonio.tasks.Constants.GraphQL.Inputs.TaskInput;
import com.zextras.carbonio.tasks.dal.dao.Priority;
import com.zextras.carbonio.tasks.dal.dao.Status;
import com.zextras.carbonio.tasks.dal.dao.Task;
import com.zextras.carbonio.tasks.dal.repositories.TaskRepository;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TaskDataFetchers {

  private final TaskRepository taskRepository;

  @Inject
  public TaskDataFetchers(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  public DataFetcher<CompletableFuture<DataFetcherResult<Map<String, Object>>>> createTask() {
    return environment ->
        CompletableFuture.supplyAsync(
            () -> {
              String userId = environment.getGraphQlContext().get(Context.REQUESTER_ID);
              Map<String, Object> newTask = environment.getArgument(Inputs.NEW_TASK);

              String title = (String) newTask.get(TaskInput.TITLE);
              String description = (String) newTask.get(TaskInput.DESCRIPTION);
              Priority priority = (Priority) newTask.get(TaskInput.PRIORITY);
              Status status = (Status) newTask.get(TaskInput.STATUS);
              // If the reminderAt is zero then it is considered as null because the reset of a
              // reminder during a creation of a task has any sense
              Long reminderAt = (Long) newTask.get(TaskInput.REMINDER_AT);
              Boolean reminderAllDay = (Boolean) newTask.get(TaskInput.REMINDER_ALL_DAY);

              Task createdTask =
                  taskRepository.createTask(
                      userId,
                      title,
                      description,
                      priority == null ? Priority.MEDIUM : priority,
                      status == null ? Status.OPEN : status,
                      reminderAt == null || reminderAt == Inputs.REMINDER_AT_RESET_VALUE
                          ? null
                          : Instant.ofEpochMilli(reminderAt),
                      reminderAllDay);

              return DataFetcherResult.<Map<String, Object>>newResult()
                  .data(convertTaskToMap(createdTask))
                  .build();
            });
  }

  public DataFetcher<CompletableFuture<DataFetcherResult<Map<String, Object>>>> updateTask() {
    return environment ->
        CompletableFuture.supplyAsync(
            () -> {
              String userId = environment.getGraphQlContext().get(Context.REQUESTER_ID);
              Map<String, Object> updateTask = environment.getArgument(Inputs.UPDATE_TASK);
              String taskId = (String) updateTask.get(TaskInput.ID);
              Optional<Task> optTask = taskRepository.getTask(UUID.fromString(taskId), userId);

              if (optTask.isEmpty()) {
                return DataFetcherResult.<Map<String, Object>>newResult()
                    .error(
                        GraphqlErrorBuilder.newError()
                            .message(String.format("Could not find task with id %s", taskId))
                            .build())
                    .build();
              }

              Task taskToUpdate = optTask.get();

              String title = (String) updateTask.get(TaskInput.TITLE);
              String description = (String) updateTask.get(TaskInput.DESCRIPTION);
              Priority priority = (Priority) updateTask.get(TaskInput.PRIORITY);
              Status status = (Status) updateTask.get(TaskInput.STATUS);
              Long reminderAt = (Long) updateTask.get(TaskInput.REMINDER_AT);
              Boolean reminderAllDay = (Boolean) updateTask.get(TaskInput.REMINDER_ALL_DAY);

              if (title != null) taskToUpdate.setTitle(title);
              if (description != null) taskToUpdate.setDescription(description);
              if (priority != null) taskToUpdate.setPriority(priority);
              if (status != null) taskToUpdate.setStatus(status);

              if (reminderAt != null && reminderAllDay != null) {
                if (reminderAt == Inputs.REMINDER_AT_RESET_VALUE) {
                  // Reset the reminderAt and the reminderAllDay
                  taskToUpdate.setReminderAt(null);
                  taskToUpdate.setReminderAllDay(null);
                } else {
                  taskToUpdate.setReminderAt(Instant.ofEpochMilli(reminderAt));
                  taskToUpdate.setReminderAllDay(reminderAllDay);
                }
              }

              taskRepository.updateTask(taskToUpdate);

              return DataFetcherResult.<Map<String, Object>>newResult()
                  .data(convertTaskToMap(taskToUpdate))
                  .build();
            });
  }

  public DataFetcher<CompletableFuture<DataFetcherResult<Map<String, Object>>>> getTask() {
    return environment ->
        CompletableFuture.supplyAsync(
            () -> {
              String userId = environment.getGraphQlContext().get(Context.REQUESTER_ID);
              UUID taskId = UUID.fromString(environment.getArgument(Inputs.TASK_ID));
              return taskRepository
                  .getTask(taskId, userId)
                  .map(
                      task ->
                          DataFetcherResult.<Map<String, Object>>newResult()
                              .data(convertTaskToMap(task))
                              .build())
                  .orElse(
                      DataFetcherResult.<Map<String, Object>>newResult()
                          .error(
                              GraphqlErrorBuilder.newError()
                                  .message(String.format("Could not find task with id %s", taskId))
                                  .build())
                          .build());
            });
  }

  public DataFetcher<CompletableFuture<List<Map<String, Object>>>> findTasks() {
    return environment ->
        CompletableFuture.supplyAsync(
            () -> {
              String userId = environment.getGraphQlContext().get(Context.REQUESTER_ID);
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
              taskMapBuilder.put(GraphQL.Task.REMINDER_AT, reminderAt.toEpochMilli());
              taskMapBuilder.put(
                  GraphQL.Task.REMINDER_ALL_DAY, task.getReminderAllDay().orElse(false));
            });

    return taskMapBuilder.build();
  }
}
