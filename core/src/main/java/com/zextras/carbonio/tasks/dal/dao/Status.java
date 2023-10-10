// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal.dao;

/**
 * Represents the status of a {@link Task}. A task status could be {@link #OPEN} or {@link
 * #COMPLETE}.
 */
public enum Status {
  OPEN,
  COMPLETE,
  TRASH
}
