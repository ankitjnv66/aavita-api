package com.aavita.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "device_digital_pins",
       uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "pin_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceDigitalPin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "pin_number", nullable = false)
    private Byte pinNumber;

    @Column(nullable = false)
    private Byte state;

    @Column(name = "updated_on", nullable = false)
    private Instant updatedOn;

    public void setUpdatedOn(Instant updatedOn) { this.updatedOn = updatedOn; }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedOn = Instant.now();
    }
}
