package com.aavita.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "device_commands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "pkt_type", nullable = false)
    private Integer pktType;

    @Column(name = "action_cause", nullable = false)
    private Byte actionCause;

    @Column(name = "serialized_payload", nullable = false, columnDefinition = "text")
    private String serializedPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_payload", columnDefinition = "jsonb")
    private String jsonPayload;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;

    @Column(name = "executed_on")
    private Instant executedOn;

    @PrePersist
    protected void onCreate() {
        if (createdOn == null) createdOn = Instant.now();
    }
}
