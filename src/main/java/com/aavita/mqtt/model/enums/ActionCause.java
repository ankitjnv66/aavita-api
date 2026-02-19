package com.aavita.mqtt.model.enums;

public enum ActionCause {
    Manual(1),
    Remote(2),
    App(3),
    Voice(4),
    NA(5);

    private final int value;

    ActionCause(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ActionCause fromValue(int value) {
        for (ActionCause c : values()) {
            if (c.value == value) return c;
        }
        return NA;
    }
}
