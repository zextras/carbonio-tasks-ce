// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.rest.controllers;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/health")
public interface HealthController {

  @GET
  @Produces("application/json")
  Response health();

  @GET
  @Path("ready")
  Response healthReady();

  @GET
  @Path("live")
  Response healthLive();
}
