package com.brevity.rpc.protocol;

import lombok.Getter;

public enum MsgType {
    REQUEST(1),
    RESPONSE(2),
    HEARTBEAT_PING(3),
    HEARTBEAT_PONG(4);

    @Getter
    private final int type;

    MsgType(int type) {
        this.type = type;
    }

    public static MsgType findByType(int type) {
        for (MsgType msgType : MsgType.values()) {
            if (type == msgType.getType()) {
                return msgType;
            }
        }
        return null;
    }
}
