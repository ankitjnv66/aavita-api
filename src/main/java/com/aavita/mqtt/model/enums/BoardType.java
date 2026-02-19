package com.aavita.mqtt.model.enums;

public enum BoardType {
    Independent(1),
    Composite(2),
    Invalid(255);

    private final int value;

    BoardType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static BoardType fromValue(int value) {
        for (BoardType b : values()) {
            if (b.value == value) return b;
        }
        return Invalid;
    }
}
