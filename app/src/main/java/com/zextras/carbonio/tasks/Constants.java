// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

/** Application-wide constants for carbonio-tasks-ce. */
public final class Constants {

  private Constants() {}

  public static final class Tasks {

    public static final String SERVICE_NAME = "carbonio-tasks";
    public static final String VERSION = "0.0.1";
    public static final String FLAVOUR = "community edition";

    private Tasks() {}
  }

  public static final class Config {

    public static final String ACCEPTED_COOKIE_TYPE = "ZM_AUTH_TOKEN";

    private Config() {}
  }

  public static final class GraphQL {

    private GraphQL() {}

    public static final class Context {

      public static final String REQUESTER_ID = "requesterId";

      private Context() {}
    }

    public static final class ServiceInfo {

      public static final String NAME = "name";
      public static final String VERSION = "version";
      public static final String FLAVOUR = "flavour";

      private ServiceInfo() {}
    }

    public static final class Task {

      public static final String ID = "id";
      public static final String TITLE = "title";
      public static final String DESCRIPTION = "description";
      public static final String PRIORITY = "priority";
      public static final String STATUS = "status";
      public static final String CREATED_AT = "createdAt";
      public static final String REMINDER_AT = "reminderAt";
      public static final String REMINDER_ALL_DAY = "reminderAllDay";

      private Task() {}
    }

    public static final class Inputs {

      public static final int TITLE_MAX_LENGTH = 1024;
      public static final int DESCRIPTION_MAX_LENGTH = 4096;
      public static final int REMINDER_AT_RESET_VALUE = 0;
      public static final String PRIORITY = "priority";
      public static final String STATUS = "status";
      public static final String TASK_ID = "taskId";
      public static final String NEW_TASK = "newTask";
      public static final String UPDATE_TASK = "updateTask";

      private Inputs() {}

      public static final class TaskInput {

        public static final String ID = Task.ID;
        public static final String TITLE = Task.TITLE;
        public static final String DESCRIPTION = Task.DESCRIPTION;
        public static final String PRIORITY = Task.PRIORITY;
        public static final String STATUS = Task.STATUS;
        public static final String REMINDER_AT = Task.REMINDER_AT;
        public static final String REMINDER_ALL_DAY = Task.REMINDER_ALL_DAY;

        private TaskInput() {}
      }
    }

    public static final class ErrorMessages {

      public static final String TASK_NOT_FOUND = "Could not find task with id %s";

      private ErrorMessages() {}
    }
  }

  public static final class DatabaseSchema {

    private DatabaseSchema() {}

    public static final class Tables {

      public static final String TASK = "task";

      private Tables() {}

      public static final class Task {

        public static final String ID = "id";
        public static final String USER_ID = "user_id";
        public static final String TITLE = "title";
        public static final String DESCRIPTION = "description";
        public static final String PRIORITY = "priority";
        public static final String STATUS = "status";
        public static final String CREATED_AT = "created_at";
        public static final String REMINDER_AT = "reminder_at";
        public static final String REMINDER_ALL_DAY = "reminder_all_day";

        private Task() {}
      }
    }
  }
}
