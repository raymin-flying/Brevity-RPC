package com.brevity.rpc.registry;

import com.brevity.rpc.common.RpcRequest;
import com.brevity.rpc.common.ServiceMeta;
import java.util.List;

public class EurekaRegistryService implements RegistryService {
    public EurekaRegistryService(String registryAddr) {

    }

    @Override
    public void register(ServiceMeta serviceMeta) throws Exception {

    }

    @Override
    public void unregister(ServiceMeta serviceMeta) throws Exception {

    }

    @Override
    public List<ServiceMeta> lookupService(RpcRequest rpcRequest) throws Exception {
        return null;
    }

    @Override
    public void notifyListener(String serviceKey) throws Exception {

    }

    @Override
    public void destory() throws Exception {

    }
}
