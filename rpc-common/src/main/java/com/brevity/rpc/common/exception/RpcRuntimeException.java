package com.brevity.rpc.common.exception;

/**
 * RPC框架错误
 */
public class RpcRuntimeException extends RuntimeException {

    public RpcRuntimeException(RpcErrorMessageEnum rpcErrorMessageEnum) {
        super(rpcErrorMessageEnum.getMessage());
    }

    public RpcRuntimeException(RpcErrorMessageEnum rpcErrorMessageEnum, String detail) {
        super(rpcErrorMessageEnum.getMessage() + ":" + detail);
    }

    public RpcRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
