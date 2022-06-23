package com.brevity.rpc.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class RpcRequestHolder {
    // RPC调用的Request工具类，用于生成全部唯一的消息ID和异步调用结果的存储
    public static final AtomicLong REQUEST_ID_GEN = new AtomicLong(0);
    public static final AtomicLong HEARTBEAT_ID_GEN = new AtomicLong(0);
    public static final Map<Long, RpcFuture<RpcResponse>> REQUEST_MAP = new ConcurrentHashMap<>();
    public static final Map<Long, RpcFuture<RpcResponse>> HEARTBBEAT_MAP = new ConcurrentHashMap<>();
}
