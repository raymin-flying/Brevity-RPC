package com.brevity.rpc.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 用于描述PRC请求
 */
@Data
public class RpcRequest implements Serializable {
    private String serviceVersion; // 方法版本
    private String className; // 该方法所属的类
    private String methodName; // 方法名
    private Object[] params; // 参数列表
    private Class<?>[] parameterTypes; // 参数类型
}
