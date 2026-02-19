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
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class PwmCommandBuilder {

    private final Random random = new Random();

    public DevicePayload build(int pin, byte value, Device device) {
        if (pin < 1 || pin > 4) {
            throw new IllegalArgumentException("PWM index must be in range 1–4");
        }

        UartCommandPacket uart = new UartCommandPacket();
        uart.setDigitalValues(new byte[18]);
        byte[] pwmValues = new byte[4];
        pwmValues[pin - 1] = value;
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
        payload.setPayloadData(pd);

        return payload;
    }
}
