// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.rest;

import com.zextras.carbonio.tasks.Constants.Service.API.Endpoints;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath(Endpoints.REST)
public class RestApplication extends Application {}
