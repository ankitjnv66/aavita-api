package com.aavita.mqtt;

import com.aavita.mqtt.model.PayloadData;
import com.aavita.mqtt.model.RoutingData;
import com.aavita.mqtt.model.DevicePayload;
import com.aavita.mqtt.model.UartCommandPacket;
import com.aavita.mqtt.model.enums.ActionCause;
import com.aavita.mqtt.model.enums.BoardType;
import com.aavita.mqtt.model.enums.DeviceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class DevicePacketDecoder {

    private static final Logger log = LoggerFactory.getLogger(DevicePacketDecoder.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static DevicePayload decode(byte[] data) {
        try {
            if (data == null || data.length == 0) {
                log.error("Decode failed: incoming data is NULL or EMPTY");
                return null;
            }
            log.info("Incoming payload size: {} bytes", data.length);

            String text = new String(data, StandardCharsets.UTF_8);
            if (looksLikeJson(text)) {
                log.info("Detected JSON payload");
                return decodeJson(text);
            }

            log.info("Payload detected as Binary frame");
            return decodeBinary(data);
        } catch (Exception ex) {
            log.error("Device decode error: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private static boolean looksLikeJson(String text) {
        if (text == null || text.isBlank()) return false;
        text = text.trim();
        return text.startsWith("{") || text.startsWith("[");
    }

    private static DevicePayload decodeJson(String json) throws Exception {
        return objectMapper.readValue(json, DevicePayload.class);
    }

    private static DevicePayload decodeBinary(byte[] data) {
        try {
            int index = 0;
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(java.nio.ByteOrder.BIG_ENDIAN);

            DevicePayload payload = new DevicePayload();
            payload.setRoutingData(new RoutingData());
            payload.setPayloadData(new PayloadData());

            // pktType (2 bytes big-endian)
            payload.getRoutingData().setPktType(buffer.getShort(index) & 0xFFFF);
            index += 2;
            log.info("pktType: {}", payload.getRoutingData().getPktType());

            // meshId (8 bytes ASCII)
            String meshId = new String(data, index, 8, StandardCharsets.US_ASCII).trim();
            payload.getRoutingData().setMeshId(meshId);
            index += 8;
            log.info("meshId: {}", meshId);

            // MACs (6 bytes each)
            payload.getRoutingData().setSrcMac(readMac(data, index));
            index += 6;
            log.info("SrcMac: {}", payload.getRoutingData().getSrcMac());

            payload.getRoutingData().setDstMac(readMac(data, index));
            index += 6;
            log.info("DstMac: {}", payload.getRoutingData().getDstMac());

            payload.getRoutingData().setGatewayMac(readMac(data, index));
            index += 6;
            log.info("GatewayMac: {}", payload.getRoutingData().getGatewayMac());

            payload.getRoutingData().setSubGatewayMac(readMac(data, index));
            index += 6;
            log.info("SubGatewayMac: {}", payload.getRoutingData().getSubGatewayMac());

            // pktId (2 bytes)
            payload.getRoutingData().setPktId(buffer.getShort(index) & 0xFFFF);
            index += 2;
            log.info("pktId: {}", payload.getRoutingData().getPktId());

            // payload fields
            payload.getPayloadData().setBoardType(BoardType.fromValue(data[index] & 0xFF));
            index++;
            payload.getPayloadData().setDeviceType(DeviceType.fromValue(data[index] & 0xFF));
            index++;
            payload.getPayloadData().setActionCause(ActionCause.fromValue(data[index] & 0xFF));
            index++;

            log.info("BoardType: {}, DeviceType: {}, ActionCause: {}",
                    payload.getPayloadData().getBoardType(),
                    payload.getPayloadData().getDeviceType(),
                    payload.getPayloadData().getActionCause());

            // CRC (2 bytes)
            payload.getPayloadData().setCrc16(buffer.getShort(index) & 0xFFFF);
            index += 2;
            log.info("Crc16: {}", payload.getPayloadData().getCrc16());

            // UART packet (23 bytes)
            byte[] uartBytes = new byte[23];
            System.arraycopy(data, index, uartBytes, 0, 23);
            payload.getPayloadData().setCmdPkt(UartCommandPacket.fromBytes(uartBytes));

            log.info("UART Packet DigitalValues: {}", bytesToHex(payload.getPayloadData().getCmdPkt().getDigitalValues()));
            log.info("UART Packet PwmValues: {}", bytesToHex(payload.getPayloadData().getCmdPkt().getPwmValues()));

            return payload;
        } catch (Exception ex) {
            log.error("Binary decode failed: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private static String readMac(byte[] data, int index) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (i > 0) sb.append(":");
            sb.append(String.format("%02X", data[index + i]));
        }
        return sb.toString();
    }

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X-", b));
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
    }
}
