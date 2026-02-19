package com.aavita.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JsonCommandBuilder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String build(UUID siteId, String base64Data) throws Exception {
        Map<String, String> packet = new HashMap<>();
        packet.put("siteId", siteId.toString());
        packet.put("data", base64Data);
        return objectMapper.writeValueAsString(packet);
    }
}
