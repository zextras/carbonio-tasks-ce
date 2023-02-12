// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

import com.zextras.carbonio.tasks.controllers.HealthControllerImpl;
import java.util.Collections;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/rest")
public class RESTTasks extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    return Collections.singleton(HealthControllerImpl.class);
  }
}
