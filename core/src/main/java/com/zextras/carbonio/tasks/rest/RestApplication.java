// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.rest;

import com.zextras.carbonio.tasks.rest.controllers.HealthControllerImpl;
import java.util.Collections;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/rest")
public class RestApplication extends Application {

  /**
   * Since the RESTEasy team decides to abandon the resteasy-guice dependency, the only way to make
   * the servlet aware of the controllers is to pass their implementation without using Guice.
   *
   * @return a {@link Set<Class>} containing all the controller classes.
   */
  @Override
  public Set<Class<?>> getClasses() {
    return Collections.singleton(HealthControllerImpl.class);
  }
}
