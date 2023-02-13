// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.rest.controllers;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class HealthControllerImpl implements HealthController {

  // TODO: implement the json response
  public Response health() {
    boolean databaseIsUp = true;
    return healthReady();
  }

  public Response healthLive() {
    return Response.noContent().build();
  }

  public Response healthReady() {
    boolean databaseIsUp = true;
    return databaseIsUp
        ? Response.noContent().build()
        : Response.status(Status.BAD_GATEWAY).build();
  }
}
