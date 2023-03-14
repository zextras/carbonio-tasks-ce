// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal.repositories;

import com.zextras.carbonio.tasks.dal.dao.Priority;
import com.zextras.carbonio.tasks.dal.dao.Status;
import com.zextras.carbonio.tasks.dal.dao.Task;
import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;

/** Represents all the allowed CRUD operations executable on a {@link Task} element. */
public interface TaskRepository {

  Task createTask(
      String userId,
      String title,
      @Nullable String description,
      Priority priority,
      Status status,
      @Nullable Instant reminderAt,
      @Nullable Boolean reminderAllDay);

  List<Task> getTasks(String userId, @Nullable Priority priority, @Nullable Status status);
}
