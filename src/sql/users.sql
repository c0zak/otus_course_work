CREATE TABLE users (
    user_id BIGINT UNIQUE NOT NULL,
    user_passwd VARCHAR(32),
    last_login TIMESTAMP NOT NULL,
    accepted BOOLEAN NOT NULL,
    admin BOOLEAN NOT NULL
);
