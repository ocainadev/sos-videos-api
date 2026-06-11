DROP INDEX IF EXISTS idx_webhook_events_provider_external;
CREATE INDEX idx_webhook_events_provider_external
    ON webhook_events(provider, external_id);