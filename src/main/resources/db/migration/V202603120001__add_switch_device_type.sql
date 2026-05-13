-- V202506140002__add_switch_device_type.sql
-- Adds a new ESP8266 multi-pin GPIO switch controller device
-- device_type = 4 → DEVICE_TYPE_SWITCH in GoogleSmartHomeService
-- Each DeviceDigitalPin on this device = one independent switch in Google Home

-- Step 1: Insert the new switch controller device
-- Replace site_id and user_id with your actual values
-- Run this to find them: SELECT site_id FROM sites LIMIT 1;
--                        SELECT id FROM users LIMIT 1;
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
    'SWITCH01',                  -- meshId — must match ESP8266 firmware MESH_ID
    'AA:BB:CC:DD:EE:31',         -- src_mac — ESP8266 MAC address
    'AA:BB:CC:DD:EE:FF',         -- dst_mac
    '11:22:33:44:55:66',         -- gateway_mac — must match ESP8266 firmware GATEWAY_MAC
    '23:AB:CD:00:F0:DE',         -- sub_gateway_mac
    0,                           -- pkt_id
    0,                           -- board_type
    4,                           -- device_type = SWITCH (multi-pin GPIO controller)
    0,                           -- device_role
    0,                           -- last_pkt_type
    NOW(),
    NOW(),
    NOW(),
    s.user_id,
    'GPIO Switch Controller',    -- device_name (shown in Google Home as prefix)
    'Living Room'                -- room_hint
FROM sites s
LIMIT 1;

-- Step 2: Add digital pins for the new switch device
-- Each pin = one Google Home switch
-- Google Home ID will be: switch-{device_id}-{pin_number}
-- Add/remove pins based on how many switches you have wired

INSERT INTO device_digital_pins (device_id, pin_number, state, updated_on)
SELECT id, 1, 0, NOW() FROM devices WHERE src_mac = 'AA:BB:CC:DD:EE:31'
UNION ALL
SELECT id, 2, 0, NOW() FROM devices WHERE src_mac = 'AA:BB:CC:DD:EE:31'
UNION ALL
SELECT id, 3, 0, NOW() FROM devices WHERE src_mac = 'AA:BB:CC:DD:EE:31'
UNION ALL
SELECT id, 4, 0, NOW() FROM devices WHERE src_mac = 'AA:BB:CC:DD:EE:31'
UNION ALL
SELECT id, 5, 0, NOW() FROM devices WHERE src_mac = 'AA:BB:CC:DD:EE:31';

-- Verify:
-- SELECT d.id, d.src_mac, d.device_name, d.device_type, dp.pin_number, dp.state
-- FROM devices d
-- JOIN device_digital_pins dp ON dp.device_id = d.id
-- WHERE d.device_type = 4;
