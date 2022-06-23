package com.brevity.rpc.common.exception;

public class RpcServiceException extends RuntimeException {
    public RpcServiceException(RpcErrorMessageEnum rpcErrorMessageEnum) {
        super(rpcErrorMessageEnum.getMessage());
    }

    public RpcServiceException(RpcErrorMessageEnum rpcErrorMessageEnum, String detail) {
        super(rpcErrorMessageEnum.getMessage() + ":" + detail);
    }

    public RpcServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
