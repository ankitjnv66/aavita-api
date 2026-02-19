package com.aavita.mqtt.model.enums;

public enum CommandType {
    Action(1),
    Status(2),
    Reboot(3),
    FactoryReset(4),
    NA(5);

    private final int value;

    CommandType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
