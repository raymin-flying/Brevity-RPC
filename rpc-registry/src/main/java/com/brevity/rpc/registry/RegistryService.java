package com.brevity.rpc.registry;

import com.brevity.rpc.common.RpcRequest;
import com.brevity.rpc.common.ServiceMeta;

import java.util.List;

/**
 * 注册中心：实际使用的注册中心必须实现这个接口
 */
public interface RegistryService {

    void register(ServiceMeta serviceMeta) throws Exception;

    void unregister(ServiceMeta serviceMeta) throws Exception;

    List<ServiceMeta> lookupService(RpcRequest rpcRequest) throws Exception;

    /**
     * 当注册中心注册的服务列表发生变化时，更新服务列表
     */
    void notifyListener(String serviceKey) throws Exception;

    void destory() throws Exception;


}
