package com.zextras.carbonio.tasks.controllers;

import jakarta.ws.rs.core.Response;

public class HealthControllerImpl implements HealthController {

  public Response health() {
    return Response.ok().build();
  }
}
