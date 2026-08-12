package com.aavita.mqtt.util;

/**
 * CRC-16/MODBUS implementation, mirroring the ESP8266 firmware's crc16.c:
 * poly 0xA001 (reflected 0x8005), init 0xFFFF, no final XOR, LSB-first table lookup.
 */
public final class Crc16Util {

    private static final int POLY = 0xA001;
    private static final int[] TABLE = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            int crc = i;
            for (int j = 0; j < 8; j++) {
                crc = ((crc & 1) != 0) ? ((crc >>> 1) ^ POLY) : (crc >>> 1);
            }
            TABLE[i] = crc & 0xFFFF;
        }
    }

    private Crc16Util() {}

    public static int compute(byte[] data) {
        int crc = 0xFFFF;
        for (byte b : data) {
            crc = (crc >>> 8) ^ TABLE[(crc ^ (b & 0xFF)) & 0xFF];
            crc &= 0xFFFF;
        }
        return crc;
    }

    public static int highByte(int crc) {
        return (crc >> 8) & 0xFF;
    }

    public static int lowByte(int crc) {
        return crc & 0xFF;
    }
}