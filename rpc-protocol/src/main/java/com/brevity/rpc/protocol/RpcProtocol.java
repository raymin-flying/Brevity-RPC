package com.brevity.rpc.protocol;

import lombok.Data;

import java.io.Serializable;

/**
 * 自定义通信协议
 * 头部是固定协议头，body就是Request或者Reponse，包含着具体的方法调用数据，比如参数等
 */
@Data
public class RpcProtocol<T> implements Serializable {
    private MsgHeader header;
    private T body;
}
