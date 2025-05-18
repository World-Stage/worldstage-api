CREATE SCHEMA IF NOT EXISTS edge;

CREATE TABLE IF NOT EXISTS streams (
  id UUID PRIMARY KEY,
  stream_key UUID NOT NULL,
  rtmp_url VARCHAR,
  hls_url VARCHAR,
  active BOOLEAN DEFAULT false,
  status VARCHAR,
  created_ts TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_modified_ts TIMESTAMPTZ NOT NULL DEFAULT now()
);