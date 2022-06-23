package com.brevity.rpc.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class RpcResponse implements Serializable {
    private Object data; // 服务端调用方法后返回的结果就是data
    private String message; // 报错信息
}
