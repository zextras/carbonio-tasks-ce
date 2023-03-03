// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

public final class Constants {
  private Constants() {}

  public static final class Service {
    public static final String IP = "127.78.0.16";
    public static final int PORT = 10_000;

    private Service() {}

    public static final class API {
      private API() {}

      public static final class Endpoints {
        public static final String GRAPHQL = "/graphql/";
        public static final String REST = "/rest/";

        private Endpoints() {}
      }
    }
  }
}
