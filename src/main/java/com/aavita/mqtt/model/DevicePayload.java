package com.aavita.mqtt.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DevicePayload {

    @JsonProperty("RoutingData")
    private RoutingData routingData;

    @JsonProperty("PayloadData")
    private PayloadData payloadData;

    public RoutingData getRoutingData() { return routingData; }
    public void setRoutingData(RoutingData routingData) { this.routingData = routingData; }
    public PayloadData getPayloadData() { return payloadData; }
    public void setPayloadData(PayloadData payloadData) { this.payloadData = payloadData; }
}
