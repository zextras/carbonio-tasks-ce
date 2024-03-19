// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.config.providers.utils;

import java.net.URL;

public class Utils {
  public static boolean isScriptInPath(String filename, String relativePath) {
    URL resourceUrl = Utils.class.getClassLoader().getResource(relativePath + "/" + filename);
    return resourceUrl != null;
  }
}
