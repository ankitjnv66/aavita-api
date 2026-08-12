package com.aavita.mqtt.util;

import com.aavita.mqtt.model.PayloadData;

import java.io.ByteArrayOutputStream;

/**
 * Builds the exact byte sequence the CRC16 checksum spans, per spec:
 * boardType, deviceType, actionCause, cmdType, digitalValues[], pwmValues[]
 * (does NOT include UartCommandPacket.reserved — outside the spec'd range).
 */
public final class PayloadCrcByteBuilder {

    private PayloadCrcByteBuilder() {}

    public static byte[] build(PayloadData pd) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        out.write(pd.getBoardType().getValue());
        out.write(pd.getDeviceType().getValue());
        out.write(pd.getActionCause().getValue());
        out.write(pd.getCmdType().getValue());
        out.writeBytes(pd.getCmdPkt().getDigitalValues());
        out.writeBytes(pd.getCmdPkt().getPwmValues());

        return out.toByteArray();
    }
}