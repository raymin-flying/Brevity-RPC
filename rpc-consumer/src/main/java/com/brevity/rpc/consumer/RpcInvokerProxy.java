package com.brevity.rpc.consumer;

import com.brevity.rpc.common.*;
import com.brevity.rpc.common.exception.RpcErrorMessageEnum;
import com.brevity.rpc.common.exception.RpcServiceException;
import com.brevity.rpc.protocol.MsgHeader;
import com.brevity.rpc.protocol.MsgType;
import com.brevity.rpc.protocol.ProtocolConstants;
import com.brevity.rpc.protocol.RpcProtocol;
import com.brevity.rpc.registry.RegistryService;
import com.brevity.rpc.serialization.SerializationTypeEnum;
import io.netty.channel.DefaultEventLoop;
import io.netty.util.concurrent.DefaultPromise;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 代理类
 */
public class RpcInvokerProxy implements InvocationHandler {

    private final String serviceVersion;
    private final long timeout;
    private final RegistryService registryService;
    private final RpcConsumer rpcConsumer;
    private final SerializationTypeEnum serializationType;

    public RpcInvokerProxy(String serviceVersion, long timeout, RegistryService registryService,
                           RpcConsumer rpcConsumer, SerializationTypeEnum serializationType) {
        this.serviceVersion = serviceVersion;
        this.timeout = timeout;
        this.registryService = registryService;
        this.rpcConsumer = rpcConsumer;
        this.serializationType = serializationType;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcProtocol<RpcRequest> protocol = new RpcProtocol<>();
        MsgHeader header = new MsgHeader();
        long requestId = RpcRequestHolder.REQUEST_ID_GEN.incrementAndGet();
        header.setMagic(ProtocolConstants.MAGIC);
        header.setVersion(ProtocolConstants.VERSION);
        header.setRequestID(requestId);
        header.setStatus((byte) 0x1);
        header.setSerialization((byte) serializationType.getType());
        header.setMsgType((byte) MsgType.REQUEST.getType());
        protocol.setHeader(header);

        RpcRequest request = new RpcRequest();
        request.setServiceVersion(this.serviceVersion);
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParams(args);
        protocol.setBody(request);

        RpcFuture<RpcResponse> future = new RpcFuture<>(new DefaultPromise<>(new DefaultEventLoop()), timeout);
        RpcRequestHolder.REQUEST_MAP.put(requestId, future);
        try {
            rpcConsumer.sendRequest(protocol, this.registryService);
            return future.getPromise().get(future.getTimeout(), TimeUnit.SECONDS).getData();
        } catch (Exception e) {
            if (e instanceof TimeoutException) {
                throw new RpcServiceException(RpcErrorMessageEnum.SERVICE_CALL_TIMEOUT);
            }
            throw e;
        }
    }
}