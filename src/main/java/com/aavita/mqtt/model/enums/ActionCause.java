package com.aavita.mqtt.model.enums;

/**
 * Per protocol spec:
 *   1 - touch  (Manual)
 *   2 - app
 *   3 - voice
 *   4 - remote
 *   other - NA
 *
 * FIX (2026-08-13): values were previously shuffled — App was 3, Remote was 2,
 * Voice was 4 — which caused every command sent with ActionCause.App to go
 * out on the wire as value 3 ("voice" per spec) instead of 2 ("app"). Names
 * are kept as-is to avoid breaking existing references elsewhere in the
 * codebase; only the numeric values changed to match the spec.
 */
public enum ActionCause {
    Manual(1),
    App(2),
    Voice(3),
    Remote(4),
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