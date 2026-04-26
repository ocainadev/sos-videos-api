-- ─────────────────────────────────────────────────────────────────────────────
-- V2: Schema completo do SOS Videos
-- ─────────────────────────────────────────────────────────────────────────────

-- ─── users ───────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id                                    BIGSERIAL    PRIMARY KEY,
    name                                  VARCHAR(255) NOT NULL,
    email                                 VARCHAR(255) NOT NULL UNIQUE,
    cpf                                   VARCHAR(14),
    password_hash                         VARCHAR(255) NOT NULL,
    role                                  VARCHAR(20)  NOT NULL DEFAULT 'SUBSCRIBER',
    active                                BOOLEAN      NOT NULL DEFAULT TRUE,
    first_login_pending_password_change   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at                            TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ─── subscriptions ───────────────────────────────────────────────────────────
CREATE TABLE subscriptions (
    id                       BIGSERIAL   PRIMARY KEY,
    user_id                  BIGINT      NOT NULL REFERENCES users(id),
    status                   VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    start_date               DATE,
    end_date                 DATE,
    last_payment_date        TIMESTAMP,
    next_due_date            DATE,
    kiwify_subscription_id   VARCHAR(255),
    kiwify_customer_id       VARCHAR(255),
    created_at               TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ─── webhook_events ──────────────────────────────────────────────────────────
CREATE TABLE webhook_events (
    id             BIGSERIAL    PRIMARY KEY,
    provider       VARCHAR(50)  NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    external_id    VARCHAR(255),
    payload_json   TEXT         NOT NULL,
    processed      BOOLEAN      NOT NULL DEFAULT FALSE,
    error_message  TEXT,
    received_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    processed_at   TIMESTAMP
);

-- ─── drive_folders ───────────────────────────────────────────────────────────
CREATE TABLE drive_folders (
    id               BIGSERIAL   PRIMARY KEY,
    publish_date     DATE        NOT NULL UNIQUE,
    drive_folder_id  VARCHAR(255) NOT NULL,
    sync_status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    last_synced_at   TIMESTAMP,
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ─── videos ──────────────────────────────────────────────────────────────────
CREATE TABLE videos (
    id               BIGSERIAL    PRIMARY KEY,
    title            VARCHAR(500) NOT NULL,
    drive_file_id    VARCHAR(255) NOT NULL UNIQUE,
    thumbnail_url    VARCHAR(1000),
    publish_date     DATE         NOT NULL,
    mime_type        VARCHAR(100),
    file_size        BIGINT,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    expires_at       TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ─── downloads ───────────────────────────────────────────────────────────────
CREATE TABLE downloads (
    id              BIGSERIAL   PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users(id),
    video_id        BIGINT      NOT NULL REFERENCES videos(id),
    downloaded_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    download_type   VARCHAR(20) NOT NULL
);

-- ─── terms ───────────────────────────────────────────────────────────────────
CREATE TABLE terms (
    id          BIGSERIAL    PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    content     TEXT         NOT NULL,
    version     VARCHAR(50)  NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ─── term_acceptances ────────────────────────────────────────────────────────
CREATE TABLE term_acceptances (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id),
    term_id      BIGINT       NOT NULL REFERENCES terms(id),
    accepted_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    ip_address   VARCHAR(45),
    user_agent   VARCHAR(500)
);

-- ─── tutorials ───────────────────────────────────────────────────────────────
CREATE TABLE tutorials (
    id            BIGSERIAL    PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    content_type  VARCHAR(20)  NOT NULL,
    url           VARCHAR(1000),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order    INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ─── Indexes ─────────────────────────────────────────────────────────────────
CREATE INDEX idx_subscriptions_user_id   ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_status    ON subscriptions(status);
CREATE INDEX idx_subscriptions_end_date  ON subscriptions(end_date);

CREATE UNIQUE INDEX idx_webhook_events_provider_external
    ON webhook_events(provider, external_id)
    WHERE external_id IS NOT NULL;
CREATE INDEX idx_webhook_events_processed ON webhook_events(processed);

CREATE INDEX idx_videos_publish_date  ON videos(publish_date);
CREATE INDEX idx_videos_active        ON videos(active);
CREATE INDEX idx_videos_date_active   ON videos(publish_date, active);

CREATE INDEX idx_downloads_user_id    ON downloads(user_id);
CREATE INDEX idx_downloads_video_id   ON downloads(video_id);
CREATE INDEX idx_downloads_user_video ON downloads(user_id, video_id);

CREATE INDEX idx_term_acceptances_user_id  ON term_acceptances(user_id);
CREATE INDEX idx_term_acceptances_term_id  ON term_acceptances(term_id);

CREATE INDEX idx_tutorials_active_order ON tutorials(active, sort_order);
