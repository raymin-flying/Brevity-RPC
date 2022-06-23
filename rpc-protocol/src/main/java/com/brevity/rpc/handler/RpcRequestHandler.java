package com.brevity.rpc.handler;

import com.brevity.rpc.common.RpcRequest;
import com.brevity.rpc.common.RpcResponse;
import com.brevity.rpc.common.RpcServiceHelper;
import com.brevity.rpc.protocol.MsgHeader;
import com.brevity.rpc.protocol.MsgStatus;
import com.brevity.rpc.protocol.MsgType;
import com.brevity.rpc.protocol.RpcProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.reflect.FastClass;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

@Slf4j
public class RpcRequestHandler extends SimpleChannelInboundHandler<RpcProtocol<RpcRequest>> {

    private final Map<String, Object> rpcServiceMap;

    public RpcRequestHandler(Map<String, Object> rpcServiceMap) {
        this.rpcServiceMap = rpcServiceMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcProtocol<RpcRequest> protocol) {
        RpcRequestProcessor.submitRequest(() -> {
            log.info("handle request: " + protocol.getBody().toString());
            RpcProtocol<RpcResponse> responseProtocol = new RpcProtocol<>();
            RpcResponse response = new RpcResponse();
            MsgHeader header = protocol.getHeader();
            header.setMsgType((byte) MsgType.RESPONSE.getType());
            try {
                Object result = handle(protocol.getBody());
                response.setData(result);
                header.setStatus((byte) MsgStatus.SUCCESS.getCode());
            } catch (Throwable e) {
                header.setStatus((byte) MsgStatus.FAIL.getCode());
                response.setMessage(e.toString());
                log.error("process request {} error", header.getRequestID(), e);
            }
            responseProtocol.setHeader(header);
            responseProtocol.setBody(response);
            ctx.writeAndFlush(responseProtocol);
        });
    }

    private Object handle(RpcRequest request) throws InvocationTargetException {
        String serviceKey = RpcServiceHelper.buildServiceKey(request.getClassName(), request.getServiceVersion());
        Object serviceBean = rpcServiceMap.get(serviceKey);

        if (serviceBean == null) {
            throw new RuntimeException(String.format("service not exist: {}:{}", request.getClassName(), request.getServiceVersion()));
        }

        Class<?> serviceBeanClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] params = request.getParams();

        FastClass fastClass = FastClass.create(serviceBeanClass);
        int methodIndex = fastClass.getIndex(methodName, parameterTypes);
        return fastClass.invoke(methodIndex, serviceBean, params);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                log.info("no interaction with client after 90s: {}, disconnected...", ctx.channel().remoteAddress());
                ctx.channel().close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("RPC server catch some exception");
        cause.printStackTrace();
        System.out.println("client abnormal disconnect：" + ctx.channel().remoteAddress());
        ctx.channel().close();
    }
}



























