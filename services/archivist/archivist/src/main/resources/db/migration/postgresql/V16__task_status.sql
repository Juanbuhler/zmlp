
ALTER TABLE task ADD COLUMN int_ping_count INT NOT NULL DEFAULT 0;
ALTER TABLE task ADD COLUMN int_progress SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE task ADD COLUMN str_status TEXT NOT NULL DEFAULT '';