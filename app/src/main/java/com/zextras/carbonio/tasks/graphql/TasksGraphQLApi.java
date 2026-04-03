// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql;

import com.zextras.carbonio.tasks.Constants.GraphQL.ErrorMessages;
import com.zextras.carbonio.tasks.Constants.GraphQL.Inputs;
import com.zextras.carbonio.tasks.Constants.Tasks;
import com.zextras.carbonio.tasks.dal.dao.Priority;
import com.zextras.carbonio.tasks.dal.dao.Status;
import com.zextras.carbonio.tasks.dal.dao.Task;
import com.zextras.carbonio.tasks.dal.repositories.TaskRepository;
import com.zextras.carbonio.tasks.graphql.types.NewTaskInput;
import com.zextras.carbonio.tasks.graphql.types.ServiceInfo;
import com.zextras.carbonio.tasks.graphql.types.TaskResponse;
import com.zextras.carbonio.tasks.graphql.types.UpdateTaskInput;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.GraphQLException;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

/**
 * SmallRye GraphQL API endpoint providing queries and mutations for carbonio-tasks-ce.
 *
 * <p>Authentication is handled by {@link com.zextras.carbonio.tasks.auth.AuthenticationFilter}
 * which populates the request-scoped {@link RequestContext} with the authenticated user's ID.
 * Input validation is done inline (previously handled by Ebean's {@code InputFieldsValidator}).
 */
@GraphQLApi
@ApplicationScoped
public class TasksGraphQLApi {

  @Inject
  TaskRepository taskRepository;

  @Inject
  RequestContext requestContext;

  // ──────────────────────────── Queries ────────────────────────────

  @Query("getServiceInfo")
  @Description("Returns service metadata: name, version and flavour.")
  public ServiceInfo getServiceInfo() {
    return new ServiceInfo(Tasks.SERVICE_NAME, Tasks.VERSION, Tasks.FLAVOUR);
  }

  @Query("getTask")
  @Description("Returns a single task by its ID for the authenticated user.")
  public TaskResponse getTask(@Name("taskId") String taskId) throws GraphQLException {
    String userId = requestContext.getRequesterId();
    UUID uuid = UUID.fromString(taskId);

    return taskRepository
        .getTask(uuid, userId)
        .map(this::toTaskResponse)
        .orElseThrow(
            () ->
                new GraphQLException(
                    String.format(ErrorMessages.TASK_NOT_FOUND, taskId),
                    GraphQLException.ExceptionType.DataFetchingException));
  }

  @Query("findTasks")
  @Description("Returns all non-trashed tasks for the authenticated user, optionally filtered.")
  public List<TaskResponse> findTasks(
      @Name("status") Status status, @Name("priority") Priority priority) {
    String userId = requestContext.getRequesterId();
    return taskRepository.getTasks(userId, priority, status).stream()
        .map(this::toTaskResponse)
        .collect(Collectors.toList());
  }

  // ──────────────────────────── Mutations ──────────────────────────

  @Mutation("createTask")
  @Description("Creates a new task for the authenticated user.")
  public TaskResponse createTask(@Name("newTask") NewTaskInput newTask) throws GraphQLException {
    validateUpsertInput(
        newTask.getTitle(),
        newTask.getDescription(),
        newTask.getReminderAt(),
        newTask.getReminderAllDay());

    String userId = requestContext.getRequesterId();

    Long reminderAtMs = newTask.getReminderAt();
    Instant reminderAt =
        (reminderAtMs == null || reminderAtMs == Inputs.REMINDER_AT_RESET_VALUE)
            ? null
            : Instant.ofEpochMilli(reminderAtMs);

    Task created =
        taskRepository.createTask(
            userId,
            newTask.getTitle(),
            newTask.getDescription(),
            newTask.getPriority() == null ? Priority.MEDIUM : newTask.getPriority(),
            newTask.getStatus() == null ? Status.OPEN : newTask.getStatus(),
            reminderAt,
            newTask.getReminderAllDay());

    return toTaskResponse(created);
  }

  @Mutation("updateTask")
  @Description("Updates an existing task. Only fields present in the input are modified.")
  public TaskResponse updateTask(@Name("updateTask") UpdateTaskInput updateTask)
      throws GraphQLException {
    validateUpsertInput(
        updateTask.getTitle(),
        updateTask.getDescription(),
        updateTask.getReminderAt(),
        updateTask.getReminderAllDay());

    String userId = requestContext.getRequesterId();
    UUID taskId = UUID.fromString(updateTask.getId());

    Task task =
        taskRepository
            .getTask(taskId, userId)
            .orElseThrow(
                () ->
                    new GraphQLException(
                        String.format(ErrorMessages.TASK_NOT_FOUND, updateTask.getId()),
                        GraphQLException.ExceptionType.DataFetchingException));

    if (updateTask.getTitle() != null) task.setTitle(updateTask.getTitle());
    if (updateTask.getDescription() != null) task.setDescription(updateTask.getDescription());
    if (updateTask.getPriority() != null) task.setPriority(updateTask.getPriority());
    if (updateTask.getStatus() != null) task.setStatus(updateTask.getStatus());

    Long reminderAtMs = updateTask.getReminderAt();
    Boolean reminderAllDay = updateTask.getReminderAllDay();

    if (reminderAtMs != null && reminderAllDay != null) {
      if (reminderAtMs == Inputs.REMINDER_AT_RESET_VALUE) {
        task.setReminderAt(null);
        task.setReminderAllDay(null);
      } else {
        task.setReminderAt(Instant.ofEpochMilli(reminderAtMs));
        task.setReminderAllDay(reminderAllDay);
      }
    }

    taskRepository.updateTask(task);
    return toTaskResponse(task);
  }

  @Mutation("trashTask")
  @Description("Moves a task to the TRASH status, making it invisible to normal queries.")
  public String trashTask(@Name("taskId") String taskId) throws GraphQLException {
    String userId = requestContext.getRequesterId();
    UUID uuid = UUID.fromString(taskId);

    Task task =
        taskRepository
            .getTask(uuid, userId)
            .orElseThrow(
                () ->
                    new GraphQLException(
                        String.format(ErrorMessages.TASK_NOT_FOUND, taskId),
                        GraphQLException.ExceptionType.DataFetchingException));

    task.setStatus(Status.TRASH);
    taskRepository.updateTask(task);
    return taskId;
  }

  // ──────────────────────────── Private helpers ─────────────────────

  private TaskResponse toTaskResponse(Task task) {
    TaskResponse r = new TaskResponse();
    r.setId(task.getId().toString());
    r.setTitle(task.getTitle());
    r.setDescription(task.getDescription().orElse(null));
    r.setPriority(task.getPriority());
    r.setStatus(task.getStatus());
    r.setCreatedAt(task.getCreatedAt().toEpochMilli());
    r.setReminderAt(task.getReminderAt().map(Instant::toEpochMilli).orElse(null));
    r.setReminderAllDay(task.getReminderAllDay().orElse(null));
    return r;
  }

  /**
   * Validates common constraints for both {@code createTask} and {@code updateTask} inputs.
   * Mirrors the logic of the old {@code InputFieldsValidator}.
   */
  private void validateUpsertInput(
      String title, String description, Long reminderAt, Boolean reminderAllDay)
      throws GraphQLException {

    StringBuilder errors = new StringBuilder();

    if (title != null && title.length() > Inputs.TITLE_MAX_LENGTH) {
      errors.append(
          String.format(
              "Invalid title. Length is more than %d characters", Inputs.TITLE_MAX_LENGTH));
      errors.append("\n");
    }

    if (description != null && description.length() > Inputs.DESCRIPTION_MAX_LENGTH) {
      errors.append(
          String.format(
              "Invalid description. Length is more than %d characters",
              Inputs.DESCRIPTION_MAX_LENGTH));
      errors.append("\n");
    }

    if ((reminderAt == null && reminderAllDay != null)
        || (reminderAt != null && reminderAllDay == null)) {
      errors.append(
          "The reminderAt and the reminderAllDay attributes must be both always set");
    }

    String errorMessage = errors.toString().stripTrailing();
    if (!errorMessage.isEmpty()) {
      throw new GraphQLException(
          errorMessage, GraphQLException.ExceptionType.DataFetchingException);
    }
  }
}
