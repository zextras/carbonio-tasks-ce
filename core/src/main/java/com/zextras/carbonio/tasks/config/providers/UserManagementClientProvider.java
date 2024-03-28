// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.config.providers;

import com.google.inject.Provider;
import com.zextras.carbonio.tasks.Constants.Config;
import com.zextras.carbonio.usermanagement.UserManagementClient;

public class UserManagementClientProvider implements Provider<UserManagementClient> {
  @Override
  public UserManagementClient get() {
    return UserManagementClient.atURL(
        Config.UserService.PROTOCOL, Config.UserService.URL, Config.UserService.PORT);
  }
}
