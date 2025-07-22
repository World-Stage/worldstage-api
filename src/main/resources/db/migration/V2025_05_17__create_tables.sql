CREATE SCHEMA IF NOT EXISTS edge;

CREATE TABLE IF NOT EXISTS streams (
  id UUID PRIMARY KEY,
  stream_key UUID NOT NULL,
  rtmp_url VARCHAR,
  hls_url VARCHAR NOT NULL,
  user_id UUID NOT NULL,
  active BOOLEAN DEFAULT false,
  status VARCHAR,
  created_ts TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_modified_ts TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Create index for streams
Create INDEX idx_stream_stream_key ON streams(stream_key);

-- Create roles table
CREATE TABLE edge.roles (
    id UUID PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL
);

-- Create users table
CREATE TABLE edge.users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(120) NOT NULL,
    stream_key UUID NOT NULL UNIQUE,
    created_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- Create index for users
Create INDEX idx_user_stream_key ON users(stream_key);

-- Create join table for users and roles
CREATE TABLE edge.users_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_users_roles_user FOREIGN KEY (user_id) REFERENCES edge.users (id) ON DELETE CASCADE,
    CONSTRAINT fk_users_roles_role FOREIGN KEY (role_id) REFERENCES edge.roles (id) ON DELETE CASCADE
);

-- Create refresh_token table
CREATE TABLE edge.refresh_token (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL,
    family_id UUID NOT NULL,
    user_id UUID NOT NULL,
    expires_at TIMESTAMP,
    created_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES edge.users (id) ON DELETE CASCADE
);