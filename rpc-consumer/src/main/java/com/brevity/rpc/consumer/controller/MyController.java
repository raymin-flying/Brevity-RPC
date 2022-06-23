package com.brevity.rpc.consumer.controller;

import com.brevity.rpc.consumer.annotation.RpcReference;
import com.brevity.rpc.facade.ArrayFacade;
import com.brevity.rpc.facade.HelloFacade;
import org.springframework.stereotype.Component;

@Component
public class MyController {
    @RpcReference(serviceVersion = "1.0.0")
    private HelloFacade helloFacade;

    @RpcReference(serviceVersion = "1.0.0")
    private ArrayFacade arrayFacade;

    public void test() throws InterruptedException {

        for(int i=0;i<10;i++){
            System.out.println(helloFacade.helloRpc("xiaoming"));
            System.out.println(helloFacade.helloRpc("xiaoming","guangzhou"));
            System.out.println(arrayFacade.hello("brevity"));
            Thread.sleep(10000);
        }
    }
}
