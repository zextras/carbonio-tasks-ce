// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.utilities;

import java.net.URL;

public class DatabaseUtils {
  public static boolean isScriptInPath(String filename, String relativePath) {
    URL resourceUrl =
        DatabaseUtils.class
            .getClassLoader()
            .getResource(String.format("%s/%s", relativePath, filename));
    return resourceUrl != null;
  }
}
