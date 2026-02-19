package com.aavita.mqtt.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DeviceMessageWrapper {

    @JsonProperty("siteId")
    private String siteId;

    @JsonProperty("data")
    private String data;  // base64 of device payload
}
