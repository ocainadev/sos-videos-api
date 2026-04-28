-- Stores free-text notes/links per publish date, editable by admin.
CREATE TABLE IF NOT EXISTS date_notes (
    id         BIGSERIAL PRIMARY KEY,
    date       DATE        NOT NULL UNIQUE,
    content    TEXT        NOT NULL DEFAULT '',
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW()
);
