package com.brevity.rpc.provider.facade;

import com.brevity.rpc.facade.HelloFacade;
import com.brevity.rpc.provider.annotation.RpcService;

@RpcService(serviceInterface = HelloFacade.class, serviceVersion = "1.0.0")
public class HelloFacadeImpl implements HelloFacade {

    @Override
    public String helloRpc(String name) {
        return "HelloFacade: " + name;
    }

    @Override
    public String helloRpc(String name, String address) {
        return "HelloFacade: hello " + name + ", my friend from " + address;
    }
}
