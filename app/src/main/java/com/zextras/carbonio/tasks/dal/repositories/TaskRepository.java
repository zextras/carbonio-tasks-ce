// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal.repositories;

import com.zextras.carbonio.tasks.dal.dao.Priority;
import com.zextras.carbonio.tasks.dal.dao.Status;
import com.zextras.carbonio.tasks.dal.dao.Task;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Panache repository implementing all CRUD operations on {@link Task} entities. Replaces the old
 * Ebean-based {@code TaskRepositoryEbean}.
 */
@ApplicationScoped
public class TaskRepository implements PanacheRepositoryBase<Task, UUID> {

  private final Clock clock;

  public TaskRepository(Clock clock) {
    this.clock = clock;
  }

  @Transactional
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
            reminderAllDay);

    persist(newTask);
    return newTask;
  }

  @Transactional
  public void updateTask(Task taskToUpdate) {
    // Use merge() for detached entities (loaded outside a transaction boundary).
    // persist() is only for new (transient) entities; it throws on detached ones.
    getEntityManager().merge(taskToUpdate);
  }

  public Optional<Task> getTask(UUID taskId, String userId) {
    return find("id = ?1 and userId = ?2 and status != ?3", taskId, userId, Status.TRASH)
        .firstResultOptional();
  }

  public List<Task> getTasks(String userId, @Nullable Priority priority, @Nullable Status status) {
    StringBuilder query = new StringBuilder("userId = ?1");
    java.util.ArrayList<Object> params = new java.util.ArrayList<>();
    params.add(userId);

    if (priority != null) {
      query.append(" and priority = ?").append(params.size() + 1);
      params.add(priority);
    }

    if (status != null) {
      query.append(" and status = ?").append(params.size() + 1);
      params.add(status);
    } else {
      query.append(" and status != ?").append(params.size() + 1);
      params.add(Status.TRASH);
    }

    query.append(" order by createdAt desc");

    return find(query.toString(), params.toArray()).list();
  }
}
