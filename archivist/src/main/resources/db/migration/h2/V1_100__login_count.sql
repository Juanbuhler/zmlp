
ALTER TABLE users ADD time_last_login BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE users ADD int_login_count INT DEFAULT 0 NOT NULL;
