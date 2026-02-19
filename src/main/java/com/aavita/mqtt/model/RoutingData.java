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
public class RoutingData {

    @JsonProperty("pktType")
    private int pktType;

    @JsonProperty("meshId")
    private String meshId;

    @JsonProperty("srcMac")
    private String srcMac;

    @JsonProperty("dstMac")
    private String dstMac;

    @JsonProperty("gatewayMac")
    private String gatewayMac;

    @JsonProperty("subGatewayMac")
    private String subGatewayMac;

    @JsonProperty("pktId")
    private int pktId;

    public int getPktType() { return pktType; }
    public void setPktType(int pktType) { this.pktType = pktType; }
    public String getMeshId() { return meshId; }
    public void setMeshId(String meshId) { this.meshId = meshId; }
    public String getSrcMac() { return srcMac; }
    public void setSrcMac(String srcMac) { this.srcMac = srcMac; }
    public String getDstMac() { return dstMac; }
    public void setDstMac(String dstMac) { this.dstMac = dstMac; }
    public String getGatewayMac() { return gatewayMac; }
    public void setGatewayMac(String gatewayMac) { this.gatewayMac = gatewayMac; }
    public String getSubGatewayMac() { return subGatewayMac; }
    public void setSubGatewayMac(String subGatewayMac) { this.subGatewayMac = subGatewayMac; }
    public int getPktId() { return pktId; }
    public void setPktId(int pktId) { this.pktId = pktId; }
}
