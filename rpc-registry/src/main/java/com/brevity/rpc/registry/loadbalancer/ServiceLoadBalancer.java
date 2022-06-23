package com.brevity.rpc.registry.loadbalancer;

import com.brevity.rpc.common.RpcRequest;

import java.util.List;

/**
 * 负载均衡接口，所有实现的负载均衡器都必须实现这个接口
 */
public interface ServiceLoadBalancer<T> {

    default T select(List<T> servers, RpcRequest rpcRequest) {
        if (servers == null || servers.size() == 0) {
            return null;
        } else if (servers.size() == 1) {
            return servers.get(0);
        } else {
            return doSelect(servers, rpcRequest);
        }
    }

    T doSelect(List<T> servers, RpcRequest rpcRequest);

}
