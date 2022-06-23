package com.brevity.rpc.consumer;

import com.brevity.rpc.common.*;
import com.brevity.rpc.common.exception.RpcErrorMessageEnum;
import com.brevity.rpc.common.exception.RpcServiceException;
import com.brevity.rpc.protocol.*;
import com.brevity.rpc.serialization.SerializationTypeEnum;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.DefaultPromise;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 响应处理器
 */
@Slf4j
public class RpcReponseHandler extends SimpleChannelInboundHandler<RpcProtocol<RpcResponse>> {

    private final ChannelProvider channelProvider;

    private final Bootstrap bootstrap;

    private final RpcConsumerConfig consumerConfig;

    public RpcReponseHandler(Bootstrap bootstrap, RpcConsumerConfig consumerConfig) {
        channelProvider = ChannelProvider.getInstance();
        this.bootstrap = bootstrap;
        this.consumerConfig = consumerConfig;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcProtocol<RpcResponse> protocol) {
        long requestID = protocol.getHeader().getRequestID();
        int msgType = (int) protocol.getHeader().getMsgType();
        if (msgType == MsgType.HEARTBEAT_PONG.getType()) { // 处理心跳pong
            RpcFuture<RpcResponse> future = RpcRequestHolder.HEARTBBEAT_MAP.remove(requestID);
            if (future != null) {
                future.getPromise().setSuccess(protocol.getBody());
            }
        } else if (msgType == MsgType.RESPONSE.getType()) { // 处理响应
            RpcFuture<RpcResponse> future = RpcRequestHolder.REQUEST_MAP.remove(requestID);
            if ((byte) MsgStatus.SUCCESS.getCode() == protocol.getHeader().getStatus()) {
                future.getPromise().setSuccess(protocol.getBody());
            } else {
                future.getPromise().setFailure(new RpcServiceException(RpcErrorMessageEnum.SERVICE_INVOKE_ERROR,
                        protocol.getBody().getMessage()));
            }
        }
    }

    /**
     * 处理心跳事件
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                int lost_time = ctx.channel().attr(RpcConsumer.LOSE_HEARTBEAT_COUNT).get();
                if (lost_time > consumerConfig.getMAX_LOSE_HEARTBEAT_COUNT()) {
                    channelProvider.setChannelRetry((InetSocketAddress) ctx.channel().remoteAddress(), true);
                    ctx.channel().close();
                } else {
                    log.info("no interaction with {}, send heart ping to it", ctx.channel().remoteAddress());
                    // 构建心跳包
                    RpcProtocol<String> protocol = new RpcProtocol<>();
                    MsgHeader header = new MsgHeader();
                    long requestId = RpcRequestHolder.HEARTBEAT_ID_GEN.incrementAndGet();
                    header.setMagic(ProtocolConstants.MAGIC);
                    header.setVersion(ProtocolConstants.VERSION);
                    header.setRequestID(requestId);
                    header.setStatus((byte) 0x1);
                    header.setSerialization((byte) SerializationTypeEnum.valueOf(consumerConfig.
                            getSerializationType()).getType());
                    header.setMsgType((byte) MsgType.HEARTBEAT_PING.getType());
                    header.setMsgLen(0);
                    String body = "ping from " + ctx.channel().localAddress();
                    header.setMsgLen(body.getBytes(StandardCharsets.UTF_8).length);
                    protocol.setHeader(header);
                    protocol.setBody(body);

                    RpcFuture<RpcResponse> rpcFuture = new RpcFuture<>(new DefaultPromise<>(new DefaultEventLoop()),
                            consumerConfig.getHEARTBEAT_TIME());
                    RpcRequestHolder.HEARTBBEAT_MAP.put(requestId, rpcFuture);
                    ctx.channel().writeAndFlush(protocol);

                    rpcFuture.getPromise().addListener(future -> {
                        try {
                            RpcResponse pongResponse = (RpcResponse) rpcFuture.getPromise().get(rpcFuture.getTimeout(), TimeUnit.SECONDS);
                            log.info("recv heart pong from {}", ctx.channel().remoteAddress());
                            ctx.channel().attr(RpcConsumer.LOSE_HEARTBEAT_COUNT).set(0);
                        } catch (Exception e) {
                            if (e instanceof TimeoutException) {
                                RpcRequestHolder.HEARTBBEAT_MAP.remove(requestId);
                                int time = ctx.channel().attr(RpcConsumer.LOSE_HEARTBEAT_COUNT).get() + 1;
                                ctx.channel().attr(RpcConsumer.LOSE_HEARTBEAT_COUNT).set(time);
                                log.warn("server: " + ctx.channel().remoteAddress() + " pong timeout: " + time);
                            } else {
                                throw e;
                            }
                        }
                    });
                }
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (channelProvider.get((InetSocketAddress) ctx.channel().remoteAddress()).isRetry()) {
            log.warn("Server: " + ctx.channel().remoteAddress() + " disconnect, try to reconnect.");
            reconnect(ctx.channel());
        } else {
            channelProvider.remove((InetSocketAddress) ctx.channel().remoteAddress());
        }
    }

    private void reconnect(Channel channel) {
        ChannelProvider.Node node = channelProvider.get((InetSocketAddress) channel.remoteAddress());
        int retry_count = node.getRetry_count();
        if (retry_count < consumerConfig.getMAX_RETRY_COUNT()) {
            retry_count += 1;
            node.setRetry_count(retry_count);
            log.warn("try to reconnect: " + channel.remoteAddress() + " retry time: " + retry_count);
            try {
                ChannelFuture future = bootstrap.connect(channel.remoteAddress());
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        if (channelFuture.isSuccess()) {
                            log.warn("Reconnect successfully! " + channelFuture.channel());
                            channelFuture.channel().attr(RpcConsumer.LOSE_HEARTBEAT_COUNT).set(0);
                            channelProvider.setChannelRetry((InetSocketAddress) channelFuture.channel().remoteAddress(),
                                    false);
                            channelProvider.put((InetSocketAddress) channelFuture.channel().remoteAddress(),
                                    channelFuture.channel());
                        } else {
                            log.warn("Reconnect failed! " + channel);
                            channel.eventLoop().schedule(new Runnable() {
                                @Override
                                public void run() {
                                    reconnect(channel);
                                }
                            }, consumerConfig.getRETRY_TIME(), TimeUnit.SECONDS);
                        }
                    }
                });
            } catch (Exception e) {
                log.error("Reconnect error " + channel);
                channelProvider.remove((InetSocketAddress) channel.remoteAddress());
                channel.close();
            }
        } else {
            // 超过最大重连次数，丢弃该连接
            channelProvider.remove((InetSocketAddress) channel.remoteAddress());
            log.warn("can't reconnect to：" + channel + ". retry end");
            channel.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            log.error(ctx.channel() + " abnormal disconnect");
            channelProvider.setChannelRetry((InetSocketAddress) ctx.channel().remoteAddress(), true);
            ctx.channel().close();
        } else {
            super.exceptionCaught(ctx, cause);
        }
    }
}
