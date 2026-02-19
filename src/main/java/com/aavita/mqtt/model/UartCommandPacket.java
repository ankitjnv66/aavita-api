package com.aavita.mqtt.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UartCommandPacket {

    private byte[] digitalValues = new byte[18];
    private byte[] pwmValues = new byte[4];
    private byte reserved;

    public byte[] toBytes() {
        byte[] buffer = new byte[23];
        int index = 0;
        System.arraycopy(digitalValues, 0, buffer, index, 18);
        index += 18;
        System.arraycopy(pwmValues, 0, buffer, index, 4);
        index += 4;
        buffer[index] = reserved;
        return buffer;
    }

    public void setDigitalValues(byte[] digitalValues) { this.digitalValues = digitalValues; }
    public void setPwmValues(byte[] pwmValues) { this.pwmValues = pwmValues; }
    public byte[] getDigitalValues() { return digitalValues; }
    public byte[] getPwmValues() { return pwmValues; }

    public static UartCommandPacket fromBytes(byte[] bytes) {
        UartCommandPacket pkt = new UartCommandPacket();
        int index = 0;
        pkt.digitalValues = new byte[18];
        System.arraycopy(bytes, index, pkt.digitalValues, 0, 18);
        index += 18;
        pkt.pwmValues = new byte[4];
        System.arraycopy(bytes, index, pkt.pwmValues, 0, 4);
        index += 4;
        pkt.reserved = bytes[index];
        return pkt;
    }
}
