// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal.dao;

/**
 * Represents the priority of a {@link Task}. A task priority could be {@link #LOW}, {@link #MEDIUM}
 * or {@link #HIGH}.
 */
public enum Priority {
  LOW,
  MEDIUM,
  HIGH
}
