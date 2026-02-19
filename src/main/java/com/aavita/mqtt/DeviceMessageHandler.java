package com.aavita.mqtt;

import com.aavita.entity.Device;
import com.aavita.mqtt.model.DeviceMessageWrapper;
import com.aavita.mqtt.model.DevicePayload;
import com.aavita.repository.DeviceRepository;
import com.aavita.service.DevicePinService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceMessageHandler implements MqttService.MqttMessageHandler {

    private final DeviceRepository deviceRepository;
    private final DevicePinService devicePinService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onMessage(String topic, String payload) {
        try {
            log.info("MQTT Received Topic={}", topic);

            String[] parts = topic.split("/");
            if (parts.length < 3) {
                log.error("Invalid MQTT topic: {}", topic);
                return;
            }

            String username = parts[0];
            String siteIdStr = parts[1];

            UUID siteId;
            try {
                siteId = UUID.fromString(siteIdStr);
            } catch (IllegalArgumentException e) {
                log.error("Invalid SiteId in topic: {}", siteIdStr);
                return;
            }

            DeviceMessageWrapper wrapper = objectMapper.readValue(payload, DeviceMessageWrapper.class);
            if (wrapper == null || wrapper.getData() == null) {
                log.error("Invalid payload wrapper JSON");
                return;
            }

            byte[] rawBytes = Base64.getDecoder().decode(wrapper.getData());
            DevicePayload decoded = DevicePacketDecoder.decode(rawBytes);
            if (decoded == null) {
                log.error("Failed to decode device packet");
                return;
            }

            String srcMac = decoded.getRoutingData().getSrcMac();
            Device device = deviceRepository.findBySrcMacAndSiteId(srcMac, siteId).orElse(null);

            if (device == null) {
                log.warn("Device not registered: MAC={} Site={}", srcMac, siteId);
                return;
            }

            // Update device metadata
            device.setGatewayMac(decoded.getRoutingData().getGatewayMac());
            device.setSubGatewayMac(decoded.getRoutingData().getSubGatewayMac());
            device.setMeshId(decoded.getRoutingData().getMeshId());
            device.setLastPktType(decoded.getRoutingData().getPktType());
            device.setLastActionCause((byte) decoded.getPayloadData().getActionCause().getValue());
            device.setLastSeen(java.time.Instant.now());
            deviceRepository.save(device);

            // Update pins
            var cmdPkt = decoded.getPayloadData() != null ? decoded.getPayloadData().getCmdPkt() : null;
            byte[] digital = (cmdPkt != null && cmdPkt.getDigitalValues() != null) ? cmdPkt.getDigitalValues() : new byte[18];
            byte[] pwm = (cmdPkt != null && cmdPkt.getPwmValues() != null) ? cmdPkt.getPwmValues() : new byte[4];

            devicePinService.updateDigitalPins(device.getId(), digital);
            devicePinService.updatePwmPins(device.getId(), pwm);

            // Save status history
            devicePinService.saveStatusHistory(device.getId(), wrapper.getData(), decoded);

            log.info("Device updated: {}", device.getId());
        } catch (Exception ex) {
            log.error("Error processing device MQTT packet", ex);
        }
    }
}
