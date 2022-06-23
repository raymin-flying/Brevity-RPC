package com.brevity.rpc.provider.facade;

import com.brevity.rpc.facade.ArrayFacade;
import com.brevity.rpc.provider.annotation.RpcService;

@RpcService(serviceInterface = ArrayFacade.class, serviceVersion = "1.0.0", weight = 50, warmup = 300000)
public class ArrayFacadeImpl implements ArrayFacade {

    @Override
    public String hello(String name) {
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "ArrayFacade: " + name;
    }
}
