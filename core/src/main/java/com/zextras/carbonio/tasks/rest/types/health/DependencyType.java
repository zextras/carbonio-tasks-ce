// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.rest.types.health;

import java.io.Serializable;

public enum DependencyType implements Serializable {
  OPTIONAL,
  REQUIRED
}
