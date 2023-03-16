// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

public final class Constants {
  private Constants() {}

  public static final class Service {

    public static final String IP = "127.78.0.16";
    public static final int PORT = 10_000;
    public static final String SERVICE_NAME = "carbonio-tasks";
    public static final String VERSION = "0.0.1";
    public static final String FLAVOUR = "community edition";

    private Service() {}

    public static final class API {
      private API() {}

      public static final class Endpoints {
        public static final String GRAPHQL = "/graphql";
        public static final String REST = "/rest";

        private Endpoints() {}
      }
    }
  }

  public static final class Config {

    public static final class Properties {
      public static final String DATABASE_URL = "carbonio.tasks.db.url";
      public static final String DATABASE_PORT = "carbonio.tasks.db.port";

      private Properties() {}
    }

    public static final class Database {

      public static final String URL = "127.78.0.16";
      public static final String PORT = "20000";
      public static final String NAME = "carbonio-tasks-db";
      public static final String USERNAME = "carbonio-tasks-db";
      // Use this constant only for testing purpose
      public static final String PASSWORD_TEST = "test";

      private Database() {}
    }

    private Config() {}
  }

  public static final class Database {

    public static final int DB_VERSION = 1;

    public static final class Tables {

      public static final String DB_INFO = "db_info";
      public static final String TASK = "task";

      public static final class DbInfo {
        public static final String VERSION = "version";

        private DbInfo() {}
      }

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

      private Tables() {}
    }

    private Database() {}
  }

  public static final class ServiceDiscover {

    public static final class Config {

      public static final class Key {

        public static final String DB_NAME = "db-name";
        public static final String DB_USERNAME = "db-username";
        public static final String DB_PASSWORD = "db-password";

        private Key() {}
      }

      private Config() {}
    }

    private ServiceDiscover() {}
  }

  public static final class GraphQL {

    public static final class Types {

      public static final String PRIORITY = "Priority";
      public static final String STATUS = "Status";

      private Types() {}
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
      public static final String PRIORITY = "priority";
      public static final String STATUS = "status";
      public static final String TASK_ID = "taskId";
      public static final String NEW_TASK = "newTask";

      public static final class NewTaskInput {

        public static final String TITLE = Task.TITLE;
        public static final String DESCRIPTION = Task.DESCRIPTION;
        public static final String PRIORITY = Task.PRIORITY;
        public static final String STATUS = Task.STATUS;
        public static final String REMINDER_AT = Task.REMINDER_AT;
        public static final String REMINDER_ALL_DAY = Task.REMINDER_ALL_DAY;

        private NewTaskInput() {}
      }

      private Inputs() {}
    }

    public static final class Queries {

      public static final String GET_SERVICE_INFO = "getServiceInfo";
      public static final String GET_TASK = "getTask";
      public static final String FIND_TASKS = "findTasks";

      private Queries() {}
    }

    public static final class Mutations {

      public static final String CREATE_TASK = "createTask";

      private Mutations() {}
    }

    private GraphQL() {}
  }
}
