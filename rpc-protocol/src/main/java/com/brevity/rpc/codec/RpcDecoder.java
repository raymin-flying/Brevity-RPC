package com.brevity.rpc.codec;

import com.brevity.rpc.common.RpcRequest;
import com.brevity.rpc.common.RpcResponse;
import com.brevity.rpc.protocol.*;
import com.brevity.rpc.serialization.RpcSerialization;
import com.brevity.rpc.serialization.SerializationFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import com.brevity.rpc.protocol.RpcProtocol;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class RpcDecoder extends ByteToMessageDecoder {

    /*
    +---------------------------------------------------------------+
    | 魔数 2byte | 协议版本号 1byte | 序列化算法 1byte | 报文类型 1byte  |
    +---------------------------------------------------------------+
    | 状态 1byte |        消息 ID 8byte     |      数据长度 4byte     |
    +---------------------------------------------------------------+
    |                   数据内容 （长度不定）                          |
    +---------------------------------------------------------------+
    */

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> list) throws Exception {
        if (in.readableBytes() < ProtocolConstants.HEADER_TOTAL_LEN) {
            return;
        }
        in.markReaderIndex();

        short magic = in.readShort();
        byte version = in.readByte();
        byte serializationType = in.readByte();
        byte msgType = in.readByte();
        byte status = in.readByte();
        long requestID = in.readLong();
        int dataLen = in.readInt();

        if (magic != ProtocolConstants.MAGIC) {
            throw new IllegalArgumentException("magic number is illegal, " + magic);
        }

        if (in.readableBytes() < dataLen) {
            in.resetReaderIndex();
            return;
        }
        byte[] data = new byte[dataLen];
        in.readBytes(data);

        MsgType msgTypeEnum = MsgType.findByType(msgType);
        if (msgTypeEnum == null) {
            return;
        }

        MsgHeader header = new MsgHeader();
        header.setMagic(magic);
        header.setVersion(version);
        header.setSerialization(serializationType);
        header.setMsgType(msgType);
        header.setStatus(status);
        header.setRequestID(requestID);
        header.setMsgLen(dataLen);

        RpcSerialization serialization = SerializationFactory.getRpcSerialization(serializationType);
        switch (msgTypeEnum) {
            case REQUEST:
                RpcRequest request = serialization.deserialize(data, RpcRequest.class);
                if (request != null) {
                    RpcProtocol<RpcRequest> protocol = new RpcProtocol<>();
                    protocol.setHeader(header);
                    protocol.setBody(request);
                    list.add(protocol);
                }
                break;
            case RESPONSE:
                RpcResponse response = serialization.deserialize(data, RpcResponse.class);
                if (response != null) {
                    RpcProtocol<RpcResponse> protocol = new RpcProtocol<>();
                    protocol.setHeader(header);
                    protocol.setBody(response);
                    list.add(protocol);
                }
                break;
            case HEARTBEAT_PING:
                log.info("recv heart ping from {}", ctx.channel().remoteAddress());
                RpcProtocol<String> protocol = new RpcProtocol<>();
                header.setMsgType((byte) MsgType.HEARTBEAT_PONG.getType());
                header.setStatus((byte) MsgStatus.SUCCESS.getCode());
                String body = "server: " + ctx.channel().localAddress() + " return a heart pong";
                header.setMsgLen(body.getBytes(StandardCharsets.UTF_8).length);
                protocol.setHeader(header);
                protocol.setBody(body);
                ctx.writeAndFlush(protocol);
                break;
            case HEARTBEAT_PONG:
                String pong = serialization.deserialize(data, String.class);
                if (pong != null) {
                    RpcProtocol<RpcResponse> protocol1 = new RpcProtocol<>();
                    protocol1.setHeader(header);
                    RpcResponse rpcResponse = new RpcResponse();
                    rpcResponse.setData(pong);
                    protocol1.setBody(rpcResponse);
                    list.add(protocol1);
                }
                break;
        }
    }
}
