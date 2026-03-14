package com.aavita.mqtt;

import com.aavita.entity.Device;
import com.aavita.entity.DeviceDigitalPin;
import com.aavita.mqtt.model.DevicePayload;
import com.aavita.mqtt.model.PayloadData;
import com.aavita.mqtt.model.RoutingData;
import com.aavita.mqtt.model.UartCommandPacket;
import com.aavita.mqtt.model.enums.ActionCause;
import com.aavita.mqtt.model.enums.BoardType;
import com.aavita.mqtt.model.enums.CommandType;
import com.aavita.mqtt.model.enums.DeviceType;
import com.aavita.repository.DeviceDigitalPinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * Builds a SET_PIN DevicePayload for a single GPIO pin toggle.
 *
 * CRITICAL FIX vs old version:
 * Old: set only target pin, all others = 0 → ESP8266 turns off all other pins!
 * New: read ALL current pin states from DB first, then only change target pin.
 *
 * ESP8266 pin state protocol:
 *   1 = ON
 *   2 = OFF
 *   0 = unchanged / not set (we avoid this — always send explicit state)
 */
@Component
@RequiredArgsConstructor
public class PinCommandBuilder {

    private static final int  DIGITAL_PIN_COUNT = 18;
    private static final byte PIN_ON            = 1;
    private static final byte PIN_OFF           = 2;

    private final DeviceDigitalPinRepository digitalPinRepository;
    private final Random random = new Random();

    /**
     * Builds a full DevicePayload with ALL 18 digital pin states.
     * Only the target pin changes — all others are preserved from DB.
     *
     * @param pin   Pin number 1–18 (1-based, matches DB pin_number)
     * @param state true = ON, false = OFF
     * @param device The ESP8266 Device entity
     */
    public DevicePayload build(int pin, boolean state, Device device) {
        if (pin < 1 || pin > DIGITAL_PIN_COUNT) {
            throw new IllegalArgumentException("Pin must be in range 1–18, got: " + pin);
        }

        // ----------------------------------------------------------------
        // Step 1: Read ALL current pin states from DB
        // This ensures we don't accidentally reset other pins to 0
        // ----------------------------------------------------------------
        byte[] digitalValues = buildCurrentDigitalStateArray(device.getId());

        // ----------------------------------------------------------------
        // Step 2: Apply ONLY the target pin change
        // ----------------------------------------------------------------
        digitalValues[pin - 1] = state ? PIN_ON : PIN_OFF;

        // ----------------------------------------------------------------
        // Step 3: Build UART packet with full state array
        // ----------------------------------------------------------------
        UartCommandPacket uart = new UartCommandPacket();
        uart.setDigitalValues(digitalValues);
        uart.setPwmValues(new byte[4]);   // PWM unchanged — all zeros = no change

        // ----------------------------------------------------------------
        // Step 4: Build full DevicePayload (same structure as existing code)
        // ----------------------------------------------------------------
        DevicePayload payload = new DevicePayload();

        RoutingData rd = new RoutingData();
        rd.setPktType(device.getLastPktType() != null ? device.getLastPktType() : 1000);
        rd.setMeshId(device.getMeshId());
        rd.setSrcMac(device.getSrcMac());
        rd.setDstMac(device.getDstMac());
        rd.setGatewayMac(device.getGatewayMac()       != null ? device.getGatewayMac()       : "");
        rd.setSubGatewayMac(device.getSubGatewayMac() != null ? device.getSubGatewayMac()     : "");
        rd.setPktId(random.nextInt(65534) + 1);
        payload.setRoutingData(rd);

        PayloadData pd = new PayloadData();
        pd.setBoardType(BoardType.fromValue(device.getBoardType() & 0xFF));
        pd.setDeviceType(DeviceType.fromValue(device.getDeviceType() & 0xFF));
        pd.setActionCause(ActionCause.App);
        pd.setCmdType(CommandType.Action);
        pd.setCmdPkt(uart);
        payload.setPayloadData(pd);

        return payload;
    }

    /**
     * Reads current DeviceDigitalPin states from DB and maps them into
     * an 18-element byte array matching ESP8266 protocol:
     *   index = pinNumber - 1
     *   value = 1 (ON) or 2 (OFF)
     *
     * Pins not yet in DB default to PIN_OFF (2) — safe default.
     */
    private byte[] buildCurrentDigitalStateArray(Long deviceId) {
        // Default all to OFF (2) — never send 0 which means "no info"
        byte[] values = new byte[DIGITAL_PIN_COUNT];
        for (int i = 0; i < DIGITAL_PIN_COUNT; i++) {
            values[i] = PIN_OFF;
        }

        List<DeviceDigitalPin> pins = digitalPinRepository.findByDevice_Id(deviceId);
        for (DeviceDigitalPin pin : pins) {
            int idx = (pin.getPinNumber() & 0xFF) - 1;   // pinNumber is 1-based, byte → unsigned
            if (idx >= 0 && idx < DIGITAL_PIN_COUNT) {
                // DB state: 1 = ON, 0 = OFF → map to ESP protocol: 1 = ON, 2 = OFF
                values[idx] = pin.getState() == 1 ? PIN_ON : PIN_OFF;
            }
        }

        return values;
    }
}