-- SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
--
-- SPDX-License-Identifier: AGPL-3.0-only

BEGIN;
CREATE TABLE IF NOT EXISTS db_info (
    version INTEGER NOT NULL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS task (
    id UUID NOT NULL PRIMARY KEY,
    user_id CHARACTER(36) NOT NULL,
    title VARCHAR(1024) NOT NULL,
    description VARCHAR(4096),
    priority VARCHAR(25) DEFAULT 'NORMAL' NOT NULL,
    status VARCHAR(25) DEFAULT 'OPEN' NOT NULL,
    created_at TIMESTAMP NOT NULL,
    reminder_at TIMESTAMP,
    reminder_all_day BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE INDEX IF NOT EXISTS task_table_index_id ON task (id);
CREATE INDEX IF NOT EXISTS task_table_index_user_id ON task (user_id);
CREATE INDEX IF NOT EXISTS task_table_index_status ON task (status);

COMMIT;
