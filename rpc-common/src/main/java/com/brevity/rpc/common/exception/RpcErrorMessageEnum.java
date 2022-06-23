package com.brevity.rpc.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public enum RpcErrorMessageEnum {

    /**
     * 调用服务的错误
     */
    CLIENT_CONNECT_SERVER_FAILURE("连接服务端失败"),
    SERVICE_CALL_FAILURE("服务调用失败"),
    SERVICE_CAN_NOT_FOUND("没有找到指定的服务"),
    SERVICE_CALL_TIMEOUT("服务调用超时"),
    REGISTERY_SERVER_ERROR("注册中心异常"),
    REGISTER_SERVICE_FAIL("注册服务失败"),
    /**
     * 服务端执行方法发生异常
     */
    SERVICE_INVOKE_ERROR("服务端执行方法出现异常");

    private final String message;
}
