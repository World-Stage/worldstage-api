CREATE TABLE IF NOT EXISTS streams (
  stream_key String PRIMARY KEY,
  rtmp_url VARCHAR,
  hls_url VARCHAR,
  active BOOLEAN DEFAULT false
);