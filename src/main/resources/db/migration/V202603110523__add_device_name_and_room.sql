-- Add friendly name and room hint to devices table
ALTER TABLE devices ADD COLUMN IF NOT EXISTS device_name VARCHAR(150);
ALTER TABLE devices ADD COLUMN IF NOT EXISTS room_hint VARCHAR(150);

-- Update existing test devices with names
UPDATE devices SET device_name = 'Smart Light',    room_hint = 'Living Room' WHERE id = 1;
UPDATE devices SET device_name = 'Smart Fan',      room_hint = 'Living Room' WHERE id = 2;
UPDATE devices SET device_name = 'Smart Thermostat', room_hint = 'Living Room' WHERE id = 3;
