// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.rest.controllers;

import javax.ws.rs.core.Response;

public class HealthControllerImpl implements HealthController {

  public Response health() {
    return Response.ok().build();
  }
}
