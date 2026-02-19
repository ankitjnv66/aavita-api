package com.aavita.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "mesh_id", nullable = false, length = 100)
    private String meshId;

    @Column(name = "src_mac", nullable = false, length = 50)
    private String srcMac;

    @Column(name = "dst_mac", nullable = false, length = 50)
    private String dstMac;

    @Column(name = "gateway_mac", nullable = false, length = 50)
    private String gatewayMac;

    @Column(name = "sub_gateway_mac", nullable = false, length = 50)
    private String subGatewayMac;

    @Column(name = "pkt_id", nullable = false)
    private Integer pktId;

    @Column(name = "board_type", nullable = false)
    private Byte boardType;

    @Column(name = "device_type", nullable = false)
    private Byte deviceType;

    @Column(name = "device_role", nullable = false)
    private Byte deviceRole;

    @Column(name = "last_action_cause")
    private Byte lastActionCause;

    @Column(name = "last_pkt_type", nullable = false)
    private Integer lastPktType;

    @Column(name = "last_crc16")
    private Integer lastCrc16;

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;

    @Column(name = "updated_on", nullable = false)
    private Instant updatedOn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DeviceDigitalPin> digitalPins = new ArrayList<>();

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DevicePwmPin> pwmPins = new ArrayList<>();

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL)
    @Builder.Default
    private List<DeviceCommand> commands = new ArrayList<>();

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL)
    @Builder.Default
    private List<DeviceStatusHistory> statusHistory = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdOn == null) createdOn = now;
        if (updatedOn == null) updatedOn = now;
        if (lastSeen == null) lastSeen = now;
        if (lastPktType == null) lastPktType = 0;
    }

    public Site getSite() { return site; }
    public List<DeviceDigitalPin> getDigitalPins() { return digitalPins; }
    public List<DevicePwmPin> getPwmPins() { return pwmPins; }
    public Integer getLastPktType() { return lastPktType; }
    public String getMeshId() { return meshId; }
    public String getSrcMac() { return srcMac; }
    public String getDstMac() { return dstMac; }
    public String getGatewayMac() { return gatewayMac; }
    public String getSubGatewayMac() { return subGatewayMac; }
    public Byte getBoardType() { return boardType; }
    public Byte getDeviceType() { return deviceType; }

    @PreUpdate
    protected void onUpdate() {
        updatedOn = Instant.now();
    }
}
