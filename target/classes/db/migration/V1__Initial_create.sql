-- Aavita API - Initial schema (matches existing PostgreSQL from C# migration)
-- If database already exists, use flyway baseline or skip this

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(150) NOT NULL,
    last_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone_number TEXT NOT NULL,
    password TEXT NOT NULL,
    type TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS sites (
    site_id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    username VARCHAR(150) NOT NULL,
    location VARCHAR(250) NOT NULL,
    created_on TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_sites_user_id ON sites(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS ix_sites_site_id ON sites(site_id);

CREATE TABLE IF NOT EXISTS devices (
    id BIGSERIAL PRIMARY KEY,
    site_id UUID NOT NULL REFERENCES sites(site_id) ON DELETE CASCADE,
    mesh_id VARCHAR(100) NOT NULL,
    src_mac VARCHAR(50) NOT NULL,
    dst_mac VARCHAR(50) NOT NULL,
    gateway_mac VARCHAR(50) NOT NULL,
    sub_gateway_mac VARCHAR(50) NOT NULL,
    pkt_id INTEGER NOT NULL,
    board_type SMALLINT NOT NULL,
    device_type SMALLINT NOT NULL,
    device_role SMALLINT NOT NULL,
    last_action_cause SMALLINT,
    last_pkt_type INTEGER,
    last_crc16 INTEGER,
    last_seen TIMESTAMPTZ NOT NULL,
    created_on TIMESTAMPTZ NOT NULL,
    updated_on TIMESTAMPTZ NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_devices_mesh_id ON devices(mesh_id);
CREATE UNIQUE INDEX IF NOT EXISTS ix_devices_site_id_src_mac ON devices(site_id, src_mac);
CREATE INDEX IF NOT EXISTS ix_devices_src_mac ON devices(src_mac);
CREATE INDEX IF NOT EXISTS ix_devices_user_id ON devices(user_id);

CREATE TABLE IF NOT EXISTS device_commands (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    pkt_type INTEGER NOT NULL,
    action_cause SMALLINT NOT NULL,
    serialized_payload TEXT NOT NULL,
    json_payload JSONB,
    status VARCHAR(50) NOT NULL,
    created_on TIMESTAMPTZ NOT NULL,
    executed_on TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS ix_device_commands_device_id ON device_commands(device_id);

CREATE TABLE IF NOT EXISTS device_digital_pins (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    pin_number SMALLINT NOT NULL,
    state SMALLINT NOT NULL,
    updated_on TIMESTAMPTZ NOT NULL,
    UNIQUE(device_id, pin_number)
);

CREATE TABLE IF NOT EXISTS device_pwm_pins (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    pin_number SMALLINT NOT NULL,
    value SMALLINT NOT NULL,
    updated_on TIMESTAMPTZ NOT NULL,
    UNIQUE(device_id, pin_number)
);

CREATE TABLE IF NOT EXISTS device_status_history (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    pkt_type INTEGER NOT NULL,
    action_cause SMALLINT NOT NULL,
    serialized_payload TEXT NOT NULL,
    json_payload JSONB,
    received_on TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_device_status_history_device_id ON device_status_history(device_id);
CREATE INDEX IF NOT EXISTS ix_users_email ON users(email);
