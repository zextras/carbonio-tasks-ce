package com.zextras.carbonio.tasks;

import com.zextras.carbonio.tasks.controllers.HealthController;
import com.zextras.carbonio.tasks.controllers.HealthControllerImpl;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.Collections;
import java.util.Set;

@ApplicationPath("/")
public class RESTTasks extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    return Collections.singleton(HealthControllerImpl.class);
  }
}
