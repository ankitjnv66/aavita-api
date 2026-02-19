package com.aavita.mqtt.model.enums;

public enum DeviceType {
    eSmSw1G(0x01),
    eSmSw2G(0x02),
    eSmSw2G1F(0x03),
    eSmSw3G1F(0x04),
    eSmSw4G1F(0x05);

    private final int value;

    DeviceType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static DeviceType fromValue(int value) {
        for (DeviceType d : values()) {
            if (d.value == value) return d;
        }
        return eSmSw1G;
    }
}
