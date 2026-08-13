package com.aavita.mqtt;

import com.aavita.entity.Device;
import com.aavita.entity.DeviceDigitalPin;
import com.aavita.entity.DevicePwmPin;
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
import com.aavita.repository.DeviceDigitalPinRepository;
import com.aavita.repository.DevicePwmPinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * Builds a STATUS_REQUEST DevicePayload (cmdType = Status), matching the
 * cloud-to-device "get status of switch + dimmer" example in the protocol
 * spec.
 *
 * Unlike PinCommandBuilder / PwmCommandBuilder — which mutate a single pin
 * and send cmdType = Action — this builder never changes device state. It
 * packages the last known digital + PWM values from the DB into the
 * request so the device can confirm/report against them.
 *
 * Per spec:
 *   digitalValues: Dx = 0 (invalid / not present), 1 (On), 2 (Off)
 *   pwmValues:     Ax in [5,255] valid; Ax < 5 is invalid
 *
 * NOTE: requires DevicePwmPinRepository.findByDevice_Id(Long) — mirrors
 * DeviceDigitalPinRepository.findByDevice_Id(Long) already used in
 * PinCommandBuilder. Add that query method if it doesn't already exist.
 */
@Component
@RequiredArgsConstructor
public class StatusRequestBuilder {

    private static final int  DIGITAL_PIN_COUNT = 18;
    private static final int  PWM_PIN_COUNT     = 4;
    private static final byte NOT_PRESENT       = 0;

    private final DeviceDigitalPinRepository digitalPinRepository;
    private final DevicePwmPinRepository pwmPinRepository;
    private final Random random = new Random();

    public DevicePayload build(Device device) {
        UartCommandPacket uart = new UartCommandPacket();
        uart.setDigitalValues(buildDigitalStateArray(device.getId()));
        uart.setPwmValues(buildPwmStateArray(device.getId()));

        DevicePayload payload = new DevicePayload();

        RoutingData rd = new RoutingData();
        rd.setPktType(device.getLastPktType() != null ? device.getLastPktType() : 1001);
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
        pd.setCmdType(CommandType.Status);
        pd.setCmdPkt(uart);

        byte[] crcBytes = PayloadCrcByteBuilder.build(pd);
        pd.setCrc16(Crc16Util.compute(crcBytes));

        payload.setPayloadData(pd);

        return payload;
    }

    /**
     * Dx = 0 (not present) for pins with no DB record;
     * 1 (On) or 2 (Off) for pins with a known state.
     */
    private byte[] buildDigitalStateArray(Long deviceId) {
        byte[] values = new byte[DIGITAL_PIN_COUNT];
        for (int i = 0; i < DIGITAL_PIN_COUNT; i++) {
            values[i] = NOT_PRESENT;
        }

        List<DeviceDigitalPin> pins = digitalPinRepository.findByDevice_Id(deviceId);
        for (DeviceDigitalPin pin : pins) {
            int idx = (pin.getPinNumber() & 0xFF) - 1;
            if (idx >= 0 && idx < DIGITAL_PIN_COUNT) {
                values[idx] = pin.getState() == 1 ? (byte) 1 : (byte) 2;
            }
        }
        return values;
    }

    /**
     * Ax = last known PWM value for pins with a DB record, 0 otherwise.
     * Consumers should treat any value < 5 as invalid per spec.
     */
    private byte[] buildPwmStateArray(Long deviceId) {
        byte[] values = new byte[PWM_PIN_COUNT];

        List<DevicePwmPin> pins = pwmPinRepository.findByDevice_Id(deviceId);
        for (DevicePwmPin pin : pins) {
            int idx = (pin.getPinNumber() & 0xFF) - 1;
            if (idx >= 0 && idx < PWM_PIN_COUNT) {
                values[idx] = pin.getValue();
            }
        }
        return values;
    }
}
