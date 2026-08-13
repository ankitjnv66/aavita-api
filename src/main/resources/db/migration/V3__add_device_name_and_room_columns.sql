-- V202603110523__add_device_name_and_room.sql
-- Idempotent + safe

-- Add columns (already good)
ALTER TABLE devices
ADD COLUMN IF NOT EXISTS device_name VARCHAR(150);

ALTER TABLE devices
ADD COLUMN IF NOT EXISTS room_hint VARCHAR(150);


-- SAFE DATA UPDATE (only update if NULL, and using stable condition)

UPDATE devices
SET device_name = 'Smart Light',
    room_hint = 'Living Room'
WHERE device_name IS NULL
  AND device_type = 1;  -- adjust based on your logic


UPDATE devices
SET device_name = 'Smart Fan',
    room_hint = 'Living Room'
WHERE device_name IS NULL
  AND device_type = 2;


UPDATE devices
SET device_name = 'Smart Thermostat',
    room_hint = 'Living Room'
WHERE device_name IS NULL
  AND device_type = 3;