package com.aavita.mqtt.model;

import com.aavita.mqtt.model.enums.ActionCause;
import com.aavita.mqtt.model.enums.BoardType;
import com.aavita.mqtt.model.enums.CommandType;
import com.aavita.mqtt.model.enums.DeviceType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayloadData {

    @JsonProperty("boardType")
    private BoardType boardType;

    @JsonProperty("deviceType")
    private DeviceType deviceType;

    @JsonProperty("actionCause")
    private ActionCause actionCause;

    @JsonProperty("cmdType")
    private CommandType cmdType;

    @JsonProperty("crc16")
    private int crc16;

    @JsonProperty("cmdPkt")
    private UartCommandPacket cmdPkt;

    public BoardType getBoardType() { return boardType; }
    public void setBoardType(BoardType boardType) { this.boardType = boardType; }
    public DeviceType getDeviceType() { return deviceType; }
    public void setDeviceType(DeviceType deviceType) { this.deviceType = deviceType; }
    public ActionCause getActionCause() { return actionCause; }
    public void setActionCause(ActionCause actionCause) { this.actionCause = actionCause; }
    public CommandType getCmdType() { return cmdType; }
    public void setCmdType(CommandType cmdType) { this.cmdType = cmdType; }
    public int getCrc16() { return crc16; }
    public void setCrc16(int crc16) { this.crc16 = crc16; }
    public UartCommandPacket getCmdPkt() { return cmdPkt; }
    public void setCmdPkt(UartCommandPacket cmdPkt) { this.cmdPkt = cmdPkt; }
}
