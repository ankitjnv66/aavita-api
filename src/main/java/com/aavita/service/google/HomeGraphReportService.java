package com.aavita.service.google;

import com.google.api.services.homegraph.v1.HomeGraphService;
import com.google.api.services.homegraph.v1.model.ReportStateAndNotificationDevice;
import com.google.api.services.homegraph.v1.model.ReportStateAndNotificationRequest;
import com.google.api.services.homegraph.v1.model.ReportStateAndNotificationResponse;
import com.google.api.services.homegraph.v1.model.StateAndNotificationPayload;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class HomeGraphReportService {

    @Value("${google.homegraph.service-account-key:classpath:certs/homegraph-service-account.json}")
    private Resource serviceAccountKey;

    @Value("${google.smarthome.agent-user-id:user-001}")
    private String agentUserId;

    private HomeGraphService homeGraphService;

    @PostConstruct
    public void init() {
        try {
            InputStream is = serviceAccountKey.getInputStream();
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(is)
                    .createScoped(Collections.singleton("https://www.googleapis.com/auth/homegraph"));

            homeGraphService = new HomeGraphService.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials)
            ).setApplicationName("Aavita Smart Home").build();

            log.info("HomeGraph service initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize HomeGraph service", e);
        }
    }

    // ----------------------------------------------------------------
    // Existing: Light / Fan
    // ----------------------------------------------------------------
    public void reportState(String deviceId, boolean on, int brightness) {
        try {
            Map<String, Object> state = new HashMap<>();
            state.put("on", on);
            state.put("brightness", brightness);
            state.put("online", true);
            doReport(deviceId, state);
            log.info("HomeGraph state reported for device: {} on={} brightness={}", deviceId, on, brightness);
        } catch (Exception e) {
            log.error("Failed to report state to HomeGraph for device: {}", deviceId, e);
        }
    }

    // ----------------------------------------------------------------
    // Existing: Thermostat
    // ----------------------------------------------------------------
    public void reportThermostatState(String deviceId, boolean on, double temperatureCelsius) {
        try {
            Map<String, Object> state = new HashMap<>();
            state.put("on", on);
            state.put("thermostatMode", on ? "heat" : "off");
            state.put("thermostatTemperatureSetpoint", temperatureCelsius);
            state.put("thermostatTemperatureAmbient", temperatureCelsius);
            state.put("online", true);
            doReport(deviceId, state);
            log.info("HomeGraph thermostat state reported for device: {} on={} temp={}°C", deviceId, on, temperatureCelsius);
        } catch (Exception e) {
            log.error("Failed to report thermostat state to HomeGraph for device: {}", deviceId, e);
        }
    }

    // ----------------------------------------------------------------
    // NEW: Switch GPIO pin — reports simple on/off state
    // deviceId format: "switch-{deviceId}-{pinNumber}"
    // ----------------------------------------------------------------
    public void reportSwitchState(String deviceId, boolean on) {
        try {
            Map<String, Object> state = new HashMap<>();
            state.put("on", on);
            state.put("online", true);
            doReport(deviceId, state);
            log.info("HomeGraph switch state reported for device: {} on={}", deviceId, on);
        } catch (Exception e) {
            log.error("Failed to report switch state to HomeGraph for device: {}", deviceId, e);
        }
    }

    // ----------------------------------------------------------------
    // Shared report helper — avoids duplicating HomeGraph boilerplate
    // ----------------------------------------------------------------
    private void doReport(String deviceId, Map<String, Object> state) throws Exception {
        Map<String, Object> states = new HashMap<>();
        states.put(deviceId, state);

        ReportStateAndNotificationRequest request = new ReportStateAndNotificationRequest()
                .setRequestId(UUID.randomUUID().toString())
                .setAgentUserId(agentUserId)
                .setPayload(new StateAndNotificationPayload()
                        .setDevices(new ReportStateAndNotificationDevice()
                                .setStates(states)));

        ReportStateAndNotificationResponse response = homeGraphService
                .devices()
                .reportStateAndNotification(request)
                .execute();

        log.debug("HomeGraph requestId: {}", response.getRequestId());
    }
}