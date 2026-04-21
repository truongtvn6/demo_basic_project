CREATE TABLE event_delivery_log (
    id            BIGSERIAL PRIMARY KEY,
    routing_key   VARCHAR(128) NOT NULL,
    source        VARCHAR(32)  NOT NULL,
    external_id   VARCHAR(256) NOT NULL,
    title         VARCHAR(512),
    delivered_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    success       BOOLEAN      NOT NULL,
    error_message VARCHAR(1024)
);

CREATE UNIQUE INDEX uk_event_delivery_source_external
    ON event_delivery_log(source, external_id);

CREATE INDEX idx_event_delivery_delivered_at
    ON event_delivery_log(delivered_at DESC);
