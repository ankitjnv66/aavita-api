package com.aavita.mqtt.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

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
    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static CommandType fromCode(int value) {
        for (CommandType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid CommandType code: " + value);
    }
}
