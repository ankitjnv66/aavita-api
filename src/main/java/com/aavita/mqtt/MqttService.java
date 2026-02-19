package com.aavita.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;

@Service
public class MqttService implements MqttCallback {

    private static final Logger log = LoggerFactory.getLogger(MqttService.class);

    @Value("${mqtt.broker:tcp://localhost:1883}")
    private String broker;

    @Value("${mqtt.client-id:JavaClient}")
    private String clientId;

    private MqttClient client;
    private volatile boolean manualDisconnect = false;
    private MqttMessageHandler messageHandler;

    public interface MqttMessageHandler {
        void onMessage(String topic, String payload);
    }

    public void setMessageHandler(MqttMessageHandler handler) {
        this.messageHandler = handler;
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public void connect() throws MqttException {
        if (client == null) {
            client = new MqttClient(broker, clientId);
            client.setCallback(this);
        }
        if (client.isConnected()) {
            log.info("MQTT client is already connected");
            return;
        }
        manualDisconnect = false;
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        client.connect(options);
        log.info("MQTT client connected successfully");
    }

    @PreDestroy
    public void disconnect() {
        manualDisconnect = true;
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
                log.info("MQTT client disconnected manually");
            } catch (Exception e) {
                log.warn("Error disconnecting MQTT", e);
            }
        }
    }

    public void publish(String topic, String payload, int qos, boolean retain) throws MqttException {
        if (!client.isConnected()) {
            connect();
        }
        MqttMessage msg = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        msg.setQos(qos);
        msg.setRetained(retain);
        client.publish(topic, msg);
        log.info("Published message to '{}': {}", topic, payload);
    }

    public void subscribe(String topic, int qos) throws MqttException {
        if (!client.isConnected()) {
            connect();
        }
        client.subscribe(topic, qos);
        log.info("Subscribed to topic '{}' with QoS {}", topic, qos);
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("Disconnected from MQTT broker: {}", cause != null ? cause.getMessage() : "unknown");
        if (!manualDisconnect) {
            try {
                Thread.sleep(5000);
                connect();
            } catch (Exception e) {
                log.error("Reconnect failed", e);
            }
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        log.info("Message received on topic '{}': {}", topic, payload);
        if (messageHandler != null) {
            try {
                messageHandler.onMessage(topic, payload);
            } catch (Exception e) {
                log.error("Error processing MQTT message", e);
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {}
}
