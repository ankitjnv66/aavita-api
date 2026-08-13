-- V202603120001__add_switch_device_type.sql
-- Idempotent + environment-safe

-- Step 1: Insert switch device safely

INSERT INTO devices (
    site_id,
    mesh_id,
    src_mac,
    dst_mac,
    gateway_mac,
    sub_gateway_mac,
    pkt_id,
    board_type,
    device_type,
    device_role,
    last_pkt_type,
    last_seen,
    created_on,
    updated_on,
    user_id,
    device_name,
    room_hint
)
SELECT
    s.site_id,
    'SWITCH01',
    'AA:BB:CC:DD:EE:31',
    'AA:BB:CC:DD:EE:FF',
    '11:22:33:44:55:66',
    '23:AB:CD:00:F0:DE',
    0,
    1,
    4,
    0,
    0,
    NOW(),
    NOW(),
    NOW(),
    s.user_id,
    'GPIO Switch Controller',
    'Living Room'
FROM sites s
WHERE NOT EXISTS (
    SELECT 1 FROM devices d
    WHERE d.src_mac = 'AA:BB:CC:DD:EE:31'
)
ORDER BY s.created_on
LIMIT 1;


-- Step 2: Insert pins safely

INSERT INTO device_digital_pins (device_id, pin_number, state, updated_on)
SELECT d.id, pins.pin_number, 0, NOW()
FROM devices d
JOIN (
    SELECT 1 AS pin_number
    UNION ALL SELECT 2
    UNION ALL SELECT 3
    UNION ALL SELECT 4
    UNION ALL SELECT 5
) pins ON TRUE
WHERE d.src_mac = 'AA:BB:CC:DD:EE:31'
AND NOT EXISTS (
    SELECT 1 FROM device_digital_pins dp
    WHERE dp.device_id = d.id
      AND dp.pin_number = pins.pin_number
);