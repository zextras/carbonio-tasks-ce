package com.zextras.carbonio.tasks.controllers;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/health")
public interface HealthController {

  @GET
  Response health();
}
