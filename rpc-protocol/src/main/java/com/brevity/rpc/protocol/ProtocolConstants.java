package com.brevity.rpc.protocol;

public class ProtocolConstants {
    /*
    +---------------------------------------------------------------+
    | 魔数 2byte | 协议版本号 1byte | 序列化算法 1byte | 报文类型 1byte  |
    +---------------------------------------------------------------+
    | 状态 1byte |        消息 ID 8byte     |      数据长度 4byte     |
    +---------------------------------------------------------------+
    |                   数据内容 （长度不定）                          |
    +---------------------------------------------------------------+
    */

    public static final int HEADER_TOTAL_LEN = 18;

    public static final short MAGIC = 0x10;

    public static final byte VERSION = 0x1;

}
