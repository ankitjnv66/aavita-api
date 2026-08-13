package com.aavita.mqtt;

import com.aavita.entity.Device;
import com.aavita.mqtt.model.DevicePayload;
import com.aavita.mqtt.model.PayloadData;
import com.aavita.mqtt.model.RoutingData;
import com.aavita.mqtt.model.UartCommandPacket;
import com.aavita.mqtt.model.enums.ActionCause;
import com.aavita.mqtt.model.enums.BoardType;
import com.aavita.mqtt.model.enums.CommandType;
import com.aavita.mqtt.model.enums.DeviceType;
import com.aavita.mqtt.util.Crc16Util;
import com.aavita.mqtt.util.PayloadCrcByteBuilder;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Builds a SET_PWM DevicePayload for a single PWM/dimmer channel.
 *
 * Per protocol spec: Ax (PWM value) is valid in [5,255]; Ax < 5 is invalid.
 *
 * FIX (2026-08-13): value is now taken as an int (0-255, unsigned) instead
 * of a signed byte. Byte.parseByte() cannot represent values above 127, so
 * the old signature made it impossible to even construct a request for
 * roughly the top half of the spec's valid range. Range is validated here
 * before the value is narrowed to a byte for the wire format.
 */
@Component
public class PwmCommandBuilder {

    private static final int PWM_MIN_VALID = 5;
    private static final int PWM_MAX_VALID = 255;

    private final Random random = new Random();

    public DevicePayload build(int pin, int value, Device device) {
        if (pin < 1 || pin > 4) {
            throw new IllegalArgumentException("PWM index must be in range 1-4");
        }
        if (value < PWM_MIN_VALID || value > PWM_MAX_VALID) {
            throw new IllegalArgumentException(
                    "PWM value must be in range [" + PWM_MIN_VALID + "," + PWM_MAX_VALID + "], got: " + value);
        }

        UartCommandPacket uart = new UartCommandPacket();
        uart.setDigitalValues(new byte[18]);
        byte[] pwmValues = new byte[4];
        pwmValues[pin - 1] = (byte) value;
        uart.setPwmValues(pwmValues);

        DevicePayload payload = new DevicePayload();
        RoutingData rd = new RoutingData();
        rd.setPktType(1000);
        rd.setMeshId(device.getMeshId());
        rd.setSrcMac(device.getSrcMac());
        rd.setDstMac(device.getDstMac());
        rd.setGatewayMac(device.getGatewayMac() != null ? device.getGatewayMac() : "");
        rd.setSubGatewayMac(device.getSubGatewayMac() != null ? device.getSubGatewayMac() : "");
        rd.setPktId(random.nextInt(65534) + 1);
        payload.setRoutingData(rd);

        PayloadData pd = new PayloadData();
        pd.setBoardType(BoardType.fromValue(device.getBoardType() & 0xFF));
        pd.setDeviceType(DeviceType.fromValue(device.getDeviceType() & 0xFF));
        pd.setActionCause(ActionCause.App);
        pd.setCmdType(CommandType.Action);
        pd.setCmdPkt(uart);

        byte[] crcBytes = PayloadCrcByteBuilder.build(pd);
        pd.setCrc16(Crc16Util.compute(crcBytes));

        payload.setPayloadData(pd);

        return payload;
    }
}