package com.aavita.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class MqttConnectionService {

    private final MqttService mqttService;
    private final DeviceMessageHandler deviceMessageHandler;

    @Value("${mqtt.subscribe-topic:+/+/sub}")
    private String subscribeTopic;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void onApplicationReady() {
        log.info("MQTT Background Service starting...");
        mqttService.setMessageHandler(deviceMessageHandler);
        ensureConnected();
    }

    private void ensureConnected() {
        int attempt = 0;
        while (!mqttService.isConnected()) {
            attempt++;
            try {
                log.info("MQTT connecting... Attempt {}", attempt);
                mqttService.connect();
                log.info("MQTT connected.");
                mqttService.subscribe(subscribeTopic, 0);
                log.info("Subscribed to MQTT inbound topic: {}", subscribeTopic);
                break;
            } catch (Exception ex) {
                log.error("MQTT connection failed. Retrying in 3 seconds...", ex);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
